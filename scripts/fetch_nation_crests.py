# -*- coding: utf-8 -*-
"""
Downloads real national-team federation crests (not flags) for every fc26.db nation that has
at least one player, saved to assets/badges/nations/{nationality_id}.png.

Source: Wikipedia's "<Country> national football team" article — find candidate crest/logo
images via prop=images, then resolve each candidate's actual thumbnail URL via the imageinfo
API (iiurlwidth) rather than hand-computing the Wikimedia hash-bucket path, which 400s/404s
unless the exact allowed width is guessed. (TheSportsDB's free tier no longer returns
strTeamBadge at all, confirmed dead for this pass.)

Usage:
    python scripts/fetch_nation_crests.py
"""
import json
import os
import sqlite3
import time
import urllib.parse
import urllib.request

BASE = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets")
DB_PATH = os.path.join(BASE, "databases", "fc26.db")
OUT_DIR = os.path.join(BASE, "badges", "nations")
LOG_PATH = os.path.join(os.path.dirname(__file__), "nation_crest_log.json")

os.makedirs(OUT_DIR, exist_ok=True)

HEADERS = {"User-Agent": "Mozilla/5.0 (compatible; COPAArenaBadgeFetcher/1.0)"}


def http_get(url, timeout=15, retries=3):
    last_err = None
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers=HEADERS)
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                return resp.read()
        except Exception as e:
            last_err = e
            time.sleep(0.8 * (attempt + 1))
    raise last_err


def is_png(data: bytes) -> bool:
    return data[:8] == b"\x89PNG\r\n\x1a\n"


def wiki_search_title(query: str, lang="en"):
    url = f"https://{lang}.wikipedia.org/w/api.php?action=opensearch&search={urllib.parse.quote(query)}&limit=1&namespace=0&format=json"
    try:
        data = json.loads(http_get(url))
        titles = data[1]
        return titles[0] if titles else None
    except Exception:
        return None


def wiki_images(title: str, lang="en"):
    """Paginates through the full image list — pages with big match-results tables list one
    "Flag of X" icon per opponent (often 80-100+, alphabetically ahead of the real crest's
    filename), which silently starved this at the old imlimit=100/no-continue cap."""
    all_images: list[str] = []
    continue_token = None
    for _ in range(10):  # hard cap: 10 * 500 = 5000 images, more than any page will ever have
        url = (f"https://{lang}.wikipedia.org/w/api.php?action=query&titles={urllib.parse.quote(title)}"
               f"&prop=images&imlimit=500&format=json")
        if continue_token:
            url += f"&imcontinue={urllib.parse.quote(continue_token)}"
        try:
            data = json.loads(http_get(url))
        except Exception:
            break
        pages = data.get("query", {}).get("pages", {})
        for p in pages.values():
            all_images.extend(im["title"] for im in p.get("images", []))
        continue_token = data.get("continue", {}).get("imcontinue")
        if not continue_token:
            break
    return all_images


def image_thumb_bytes(file_title: str, width: int, lang="en"):
    """file_title like 'File:Argentina national football team logo.svg'. Resolves the real
    thumbnail URL via imageinfo (avoids guessing Wikimedia's allowed-width hash-bucket path)."""
    url = (f"https://{lang}.wikipedia.org/w/api.php?action=query&titles={urllib.parse.quote(file_title)}"
           f"&prop=imageinfo&iiprop=url&iiurlwidth={width}&format=json")
    try:
        data = json.loads(http_get(url))
        pages = data.get("query", {}).get("pages", {})
        for p in pages.values():
            infos = p.get("imageinfo") or []
            if not infos:
                return None
            thumb_url = infos[0].get("thumburl") or infos[0].get("url")
            if not thumb_url:
                return None
            img = http_get(thumb_url)
            if is_png(img):
                return img
            # Some files resolve to .svg/.jpg originals when no thumb renders — reject those,
            # this pipeline only wants PNG.
            return None
    except Exception:
        return None
    return None


# Wikipedia maintenance/template chrome ("ambox", "disambig"), generic trend-arrow icons
# ("increase"/"decrease"/"arrow" — ranking-change indicators reused across dozens of unrelated
# pages), and kit-sponsor logos ("adidas" etc.) all kept winning past the old filter by
# accident — caught via an MD5-duplicate audit (identical bytes across many different
# countries is a dead giveaway it's shared chrome, not a real crest).
BAD_WORDS = ("flag", "bandera", "drapeau", "bandiera", "flagge", "commons-logo", "wiki",
             "edit-icon", "icon.svg", "question_mark", "location", "map", "medal", "kit",
             "cropped", "camiseta", "shirt", "jersey",
             "ambox", "disambig", "increase", "decrease", "arrow", "africa football",
             "adidas", "nike logo", "puma logo", "umbro logo",
             "soccer ball", "soccerball", "football (ball)", "ball.svg")


CREST_KEYWORDS = ("logo", "crest", "badge", "emblem", "coat_of_arms", "escudo", "seal")


def pick_candidates(images: list[str], country_name: str) -> list[str]:
    """Only accept an image if its filename positively signals it's the team's own crest —
    either a crest keyword or the country's own name — rather than "first thing that isn't on
    a denylist". A denylist alone is whack-a-mole: pages with no real crest image still list
    dozens of generic Wikipedia template icons (red/yellow cards, trend arrows, a plain
    soccer ball, ...) that differ page to page, so blacklisting each one just surfaces the next
    generic icon rather than correctly finding nothing."""
    filtered = [im for im in images if not any(b in im.lower() for b in BAD_WORDS)]
    name_lower = country_name.lower()
    positive = [
        im for im in filtered
        if any(k in im.lower() for k in CREST_KEYWORDS) or name_lower in im.lower()
    ]
    preferred = [im for im in positive if any(k in im.lower() for k in CREST_KEYWORDS)]
    rest = [im for im in positive if im not in preferred]
    return preferred + rest


TITLE_TEMPLATES = (
    "{name} national football team",
    # Many countries have split men's/women's team articles — the plain title above is then
    # just a short disambiguation-style pointer page with almost no images of its own (e.g.
    # Sweden's plain title has exactly one image, a generic "Disambig gray.svg" icon, while
    # "Sweden men's national football team" has the real 185-image article with the crest).
    "{name} men's national football team",
    "{name} men's national soccer team",  # USA-style "soccer" naming
)


def try_wikipedia(country_name: str):
    for template in TITLE_TEMPLATES:
        title = wiki_search_title(template.format(name=country_name))
        if not title:
            continue
        images = wiki_images(title)
        if len(images) < 5:
            continue  # near-empty page — almost certainly the disambig-stub pattern, not a real article
        for im in pick_candidates(images, country_name):
            data = image_thumb_bytes(im, 300)
            if data and len(data) < 3_000_000:
                return data, im
    return None, None


def main():
    con = sqlite3.connect(DB_PATH)
    cur = con.cursor()
    cur.execute("""
        SELECT DISTINCT n.nationality_id, n.nationality_name
        FROM nations n JOIN players p ON p.nationality_id = n.nationality_id
        ORDER BY n.nationality_name ASC
    """)
    rows = cur.fetchall()
    print(f"{len(rows)} nations with players")

    results = {"wikipedia": [], "failed": [], "skipped_existing": []}

    for nid, name in rows:
        out_path = os.path.join(OUT_DIR, f"{nid}.png")
        if os.path.exists(out_path):
            results["skipped_existing"].append([nid, name])
            continue

        img, source_file = try_wikipedia(name)
        if img:
            with open(out_path, "wb") as f:
                f.write(img)
            results["wikipedia"].append([nid, name, source_file])
        else:
            results["failed"].append([nid, name])
        time.sleep(0.8)

    with open(LOG_PATH, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)

    print("wikipedia:", len(results["wikipedia"]))
    print("failed:", len(results["failed"]))
    print("skipped_existing:", len(results["skipped_existing"]))
    print("failed list:", [r[1] for r in results["failed"]])

    con.close()


if __name__ == "__main__":
    main()
