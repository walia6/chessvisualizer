#!/usr/bin/env python3

import sys
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.colors import Normalize, LinearSegmentedColormap

if len(sys.argv) == 1:
    print("Usage: python3 " + sys.argv[0] + " <output_image_path>", file=sys.stderr)
    sys.exit()

# === CONSTANTS ===
FILES = 'abcdefgh'
RANKS = '12345678'
PIECE_TYPES = ['K', 'Q', 'R', 'B', 'N', 'P']
PREFIXES = ['c', 'd']

PIECE_NAMES = {
    'K': 'KING', 'Q': 'QUEEN', 'R': 'ROOK',
    'B': 'BISHOP', 'N': 'KNIGHT', 'P': 'PAWN'
}
PREFIX_NAMES = {
    'c': "to checkmate a king.",
    'd': "to end up on in a draw."
}

HEATMAP_SHAPE = (8, 8)
HEATMAP_POWER = 1
TITLE_FONT_SIZE = 8

# === GRADIENT ===
GRADIENT = [
    [0.01176, 0.01569, 0.09804],
    [0.10196, 0.06274, 0.16470],
    [0.21176, 0.09804, 0.24313],
    [0.32156, 0.11765, 0.29803],
    [0.43921, 0.12156, 0.34117],
    [0.55294, 0.11372, 0.35686],
    [0.68235, 0.09019, 0.34901],
    [0.79215, 0.10588, 0.30980],
    [0.87843, 0.19607, 0.25882],
    [0.92941, 0.32941, 0.24313],
    [0.95294, 0.46274, 0.31764],
    [0.96078, 0.59215, 0.43137],
    [0.96470, 0.70196, 0.55294],
    [0.96862, 0.81568, 0.70980],
    [0.97647, 0.91764, 0.86274]
]
cmap = LinearSegmentedColormap.from_list("smooth_custom", np.array(GRADIENT), N=256)

# === INIT DATA ===
heatmaps = {
    f"{prefix}{piece}": np.zeros(HEATMAP_SHAPE, dtype=int)
    for prefix in PREFIXES for piece in PIECE_TYPES
}
games_processed = 0

# === HELPERS ===
def coord_to_index(coord: str):
    if len(coord) != 2 or coord[0] not in FILES or coord[1] not in RANKS:
        return None
    return 8 - int(coord[1]), FILES.index(coord[0])

def get_title(key: str) -> str:
    if key == "cK":
        return "...a KING to be checkmated on."
    return f"...a {PIECE_NAMES[key[1]]} {PREFIX_NAMES[key[0]]}"

def add_commas(n: int) -> str:
    return f"{n:,}"

# === INPUT PARSING ===
for line in sys.stdin:
    line = line.strip()
    if line == "game":
        games_processed += 1
        if games_processed % 100_000 == 0:
            print(f"[INFO] {games_processed} games processed...")
        continue
    if len(line) < 3:
        continue

    prefix, piece, coord = line[0], line[1], line[2:]
    key = f"{prefix}{piece}"
    if key not in heatmaps:
        continue

    index = coord_to_index(coord)
    if index is None:
        continue

    heatmaps[key][index] += 1

print("[SUCCESS] Finished analyzing " + str(games_processed) + " games.")

# === PLOTTING ===
fig, axes = plt.subplots(2, 6, figsize=(16, 9))
norm = Normalize(vmin=0, vmax=1)

for i, prefix in enumerate(PREFIXES):
    for j, piece in enumerate(PIECE_TYPES):
        key = f"{prefix}{piece}"
        ax = axes[i, j]
        data = heatmaps[key]
        boosted = (data / np.max(data)) ** HEATMAP_POWER if np.max(data) > 0 else data

        ax.imshow(boosted, cmap=cmap, norm=norm)
        ax.set_box_aspect(1)

        ax.set_xticks(np.arange(8))
        ax.set_yticks(np.arange(8))
        ax.set_xticklabels(list(FILES), fontsize=6, color='white')
        ax.set_yticklabels(list(reversed(RANKS)), fontsize=6, color='white')
        ax.tick_params(length=0)

        ax.set_title(get_title(key), color='white', fontsize=TITLE_FONT_SIZE)

# === GRADIENT COLORBAR ===
cax = fig.add_axes([0.80, 0.88, 0.15, 0.03])
cb = plt.colorbar(plt.cm.ScalarMappable(norm=norm, cmap=cmap), cax=cax, orientation='horizontal')
cb.set_ticks([0, 1])
cb.set_ticklabels(['LESS COMMON', 'MORE COMMON'])
cb.ax.tick_params(labelsize=8, colors='white')
cb.outline.set_edgecolor('white')
cb.ax.xaxis.set_tick_params(width=0)

# === FINAL TOUCHES ===
fig.suptitle("Most common square for...¹", fontsize=26, color='white')
fig.patch.set_facecolor('#2f2f2f')
plt.subplots_adjust(wspace=0.2, hspace=0.0, left=0.05, right=0.95, top=0.88, bottom=0.05)

fig.text(0.98, 0.01, f"¹{add_commas(games_processed)} games processed",
         fontsize=8, color='white', ha='right', va='bottom')

fig.savefig(sys.argv[1], dpi=100, facecolor=fig.get_facecolor())

print("[SUCCESS] Output saved to '" + sys.argv[1] + "'")