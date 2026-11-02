#!/usr/bin/env python3
"""Activity log of the Java Lab editor: replay and summary, identical to site/assets/js/lab-activity.js.

Columns are counted in UTF-16 code units, as in the browser editor.
"""


def _u16(s):
    return s.encode("utf-16-le")


def _s(b):
    return b.decode("utf-16-le", errors="replace")


def replay(events, up_to=None):
    """Applies the insert ('i') and remove ('r') events to an empty document."""
    lines = [b""]
    for e in events:
        if not isinstance(e, list) or len(e) < 2:
            continue
        if up_to is not None and e[1] > up_to:
            break
        if e[0] == "i" and len(e) >= 5:
            row, col, text = int(e[2]), int(e[3]), _u16(str(e[4]))
            while len(lines) <= row:
                lines.append(b"")
            line = lines[row]
            before, after = line[:2 * col], line[2 * col:]
            parts = _split16(text)
            if len(parts) == 1:
                lines[row] = before + text + after
            else:
                lines[row:row + 1] = [before + parts[0]] + parts[1:-1] + [parts[-1] + after]
        elif e[0] == "r" and len(e) >= 6:
            r1, c1, r2, c2 = (int(x) for x in e[2:6])
            while len(lines) <= r2:
                lines.append(b"")
            lines[r1:r2 + 1] = [lines[r1][:2 * c1] + lines[r2][2 * c2:]]
    return "\n".join(_s(l) for l in lines)


def _split16(b):
    parts, start = [], 0
    for i in range(0, len(b), 2):
        if b[i:i + 2] == b"\n\x00":
            parts.append(b[start:i])
            start = i + 2
    parts.append(b[start:])
    return parts


def fnv(text):
    h = 0x811C9DC5
    for byte in text.encode("utf-8"):
        h ^= byte
        h = (h * 0x01000193) & 0xFFFFFFFF
    return "%08x" % h


def summarise(events, code):
    s = dict(typedChars=0, insertedChars=0, removals=0, pasteBlocked=0, pasteBlockedChars=0,
             internalPastes=0, internalPasteChars=0, bulkInserts=0, bulkChars=0, dropBlocked=0,
             awayCount=0, awayMs=0, activeMs=0, loads=0, runs=0, compileErrors=0, checks=0,
             bestPassed=0, bestTotal=0, firstEditMs=None, lastEditMs=None,
             finalChars=len(_u16(code)) // 2, replayMatches=replay(events) == code)
    last_edit = away_since = None
    for e in events:
        if not isinstance(e, list) or len(e) < 2:
            continue
        kind, t = e[0], e[1]
        if kind in ("i", "r"):
            if last_edit is not None and t - last_edit < 60000:
                s["activeMs"] += t - last_edit
            last_edit = t
            if s["firstEditMs"] is None:
                s["firstEditMs"] = t
            s["lastEditMs"] = t
            if kind == "i":
                s["insertedChars"] += len(_u16(str(e[4]))) // 2
            else:
                s["removals"] += 1
        elif kind == "p":
            s["pasteBlocked"] += 1
            s["pasteBlockedChars"] += e[2]
        elif kind == "P":
            s["internalPastes"] += 1
            s["internalPasteChars"] += e[2]
        elif kind == "B":
            s["bulkInserts"] += 1
            s["bulkChars"] += e[2]
        elif kind == "d":
            s["dropBlocked"] += 1
        elif kind == "a":
            if away_since is None:
                away_since = t
                s["awayCount"] += 1
        elif kind == "f":
            if away_since is not None:
                s["awayMs"] += t - away_since
                away_since = None
        elif kind == "L":
            s["loads"] += 1
        elif kind == "x":
            s["runs"] += 1
            if e[2] == "compile-error":
                s["compileErrors"] += 1
        elif kind == "c":
            s["checks"] += 1
            if e[2] > s["bestPassed"] or s["bestTotal"] == 0:
                s["bestPassed"], s["bestTotal"] = e[2], e[3]
    s["typedChars"] = s["insertedChars"] - s["internalPasteChars"] - s["bulkChars"]
    return s


def flags(summary, trimmed=False):
    """Reasons for the teacher to look closer (not proof of anything)."""
    f = []
    if trimmed:
        f.append("typing log trimmed (very long work)")
    elif not summary["replayMatches"]:
        f.append("code differs from the typing log")
    if summary["pasteBlocked"]:
        f.append("%d paste attempt(s) blocked" % summary["pasteBlocked"])
    if summary["bulkInserts"]:
        f.append("%d large insertion(s) without typing" % summary["bulkInserts"])
    if summary["dropBlocked"]:
        f.append("%d drop attempt(s) blocked" % summary["dropBlocked"])
    if not trimmed and summary["finalChars"] > 40 and summary["typedChars"] < 0.8 * summary["finalChars"]:
        f.append("typed %d characters for %d in the code" % (summary["typedChars"], summary["finalChars"]))
    if summary["awayMs"] > 5 * 60000:
        f.append("away from the page %.0f min" % (summary["awayMs"] / 60000))
    return f
