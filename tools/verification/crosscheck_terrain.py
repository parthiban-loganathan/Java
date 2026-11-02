#!/usr/bin/env python3
"""Independent Python re-implementation of the terrain pipeline used by the Java core
(Priority-Flood + epsilon, D8, upstream-area delineation), for cross-checking.

Usage: python3 crosscheck_terrain.py ../../site/data/catchment/vellore_hills_grid.json 77 234
Prints the catchment area, longest flow path and drop; the Java test suite prints the same
quantities (TerrainAndModelTest / CourseCheck) so the two can be compared.
"""
import heapq
import json
import math
import sys

import numpy as np

DR = [-1, -1, 0, 1, 1, 1, 0, -1]
DC = [0, 1, 1, 1, 0, -1, -1, -1]


def main(path, orow, ocol):
    g = json.load(open(path))
    R, C = g["rows"], g["cols"]
    z = np.array(g["elevation"], dtype=np.float64).reshape(R, C)
    dx, dy = g["cellSizeX"], g["cellSizeY"]
    dist = [dy if DC[k] == 0 else dx if DR[k] == 0 else math.hypot(dx, dy) for k in range(8)]

    f = z.copy()
    closed = np.zeros((R, C), bool)
    heap, cnt = [], 0
    for r in range(R):
        for c in range(C):
            if r in (0, R - 1) or c in (0, C - 1):
                closed[r, c] = True
                heapq.heappush(heap, (f[r, c], cnt, r, c)); cnt += 1
    while heap:
        e, _, r, c = heapq.heappop(heap)
        for k in range(8):
            rr, cc = r + DR[k], c + DC[k]
            if 0 <= rr < R and 0 <= cc < C and not closed[rr, cc]:
                closed[rr, cc] = True
                if f[rr, cc] <= f[r, c]:
                    f[rr, cc] = np.nextafter(f[r, c], np.inf)
                heapq.heappush(heap, (f[rr, cc], cnt, rr, cc)); cnt += 1

    rec = -np.ones((R, C, 2), int)
    step = np.zeros((R, C))
    for r in range(R):
        for c in range(C):
            best, bk = 0.0, -1
            for k in range(8):
                rr, cc = r + DR[k], c + DC[k]
                if 0 <= rr < R and 0 <= cc < C:
                    s = (f[r, c] - f[rr, cc]) / dist[k]
                    if s > best:
                        best, bk = s, k
            if bk >= 0:
                rec[r, c] = (r + DR[bk], c + DC[bk]); step[r, c] = dist[bk]

    # flow length by memoised downstream walk
    fl = np.full((R, C), np.nan); fl[orow, ocol] = 0.0
    member = np.zeros((R, C), bool); member[orow, ocol] = True
    state = np.zeros((R, C), np.int8)  # 0 unknown, 1 member, 2 not member
    state[orow, ocol] = 1
    for r in range(R):
        for c in range(C):
            path = []
            rr, cc = r, c
            while state[rr, cc] == 0:
                path.append((rr, cc))
                nr, nc = rec[rr, cc]
                if nr < 0:
                    break
                rr, cc = nr, nc
            ok = state[rr, cc] == 1 and not (rec[rr, cc][0] < 0 and (rr, cc) != (orow, ocol) and state[rr, cc] == 0)
            for (pr, pc) in reversed(path):
                if ok:
                    nr, nc = rec[pr, pc]
                    fl[pr, pc] = fl[nr, nc] + step[pr, pc]; state[pr, pc] = 1
                else:
                    state[pr, pc] = 2
    m = state == 1
    area = m.sum() * dx * dy / 1e6
    far = np.unravel_index(np.nanargmax(np.where(m, fl, np.nan)), fl.shape)
    print("python: cells %d area %.3f km2 longest path %.1f m drop %.1f m" %
          (m.sum(), area, fl[far], z[far] - z[orow, ocol]))


if __name__ == "__main__":
    main(sys.argv[1], int(sys.argv[2]), int(sys.argv[3]))
