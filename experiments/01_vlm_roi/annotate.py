from pathlib import Path
import json
import matplotlib.pyplot as plt
from PIL import Image
import numpy as np

IMAGES_DIR = Path(__file__).parent / "images"
STORAGE_PATH = Path(__file__).parent / "ground_truth.json"
IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}

image_paths = sorted(
    p for p in IMAGES_DIR.iterdir()
    if p.suffix.lower() in IMAGE_EXTENSIONS
    and "Gemini" not in p.name
)

if not image_paths:
    print(f"No images found in {IMAGES_DIR}")
    exit(1)

SELECT_THRESHOLD_PX = 10


def load_json(path):
    if path.exists():
        with open(path) as f:
            return json.load(f)
    return {}


def save_json(path, data):
    with open(path, "w") as f:
        json.dump(data, f, indent=2)


def redraw(ax, lines, width, height, selected_idx=None):
    for artist in list(ax.lines):
        artist.remove()
    for txt in list(ax.texts):
        txt.remove()
    for i, line in enumerate(lines):
        is_selected = i == selected_idx
        color = "lime" if is_selected else ("cyan" if line["orientation"] == "h" else "magenta")
        lw = 3 if is_selected else 2
        if line["orientation"] == "h":
            y = line["position"] * height
            ax.axhline(y=y, color=color, linewidth=lw)
            ax.text(0.01, y, f"H{i + 1}", color=color, fontsize=9,
                    fontweight="bold", transform=ax.get_yaxis_transform())
        else:
            x = line["position"] * width
            ax.axvline(x=x, color=color, linewidth=lw)
            ax.text(x, 0.02, f"V{i + 1}", color=color, fontsize=9,
                    fontweight="bold", transform=ax.get_xaxis_transform())


def annotate_image(path, existing_lines, title_suffix=""):
    image = Image.open(path).convert("RGB")
    img_array = np.array(image)
    height, width = img_array.shape[:2]
    lines = [dict(line) for line in existing_lines]
    state = {"selected": None}

    fig, ax = plt.subplots(figsize=(12, 9))
    ax.imshow(img_array)
    ax.set_xlim(0, width)
    ax.set_ylim(height, 0)
    ax.axis("off")

    def update_title():
        sel = f"  |  selected: {'H' if lines[state['selected']]['orientation'] == 'h' else 'V'}{state['selected'] + 1}" \
            if state["selected"] is not None else ""
        ax.set_title(
            f"{path.name}{title_suffix}  |  {len(lines)} line(s){sel}\n"
            f"H=add horizontal  |  V=add vertical  |  click line=select  |  click elsewhere=move selected  |  "
            f"D=delete selected  |  U=undo last  |  close=confirm",
            fontsize=9
        )
        fig.canvas.draw_idle()

    def nearest_line(x_disp, y_disp):
        best_idx, best_dist = None, SELECT_THRESHOLD_PX
        for i, line in enumerate(lines):
            if line["orientation"] == "h":
                disp_x, disp_y = ax.transData.transform((0, line["position"] * height))
                dist = abs(y_disp - disp_y)
            else:
                disp_x, disp_y = ax.transData.transform((line["position"] * width, 0))
                dist = abs(x_disp - disp_x)
            if dist < best_dist:
                best_idx, best_dist = i, dist
        return best_idx

    redraw(ax, lines, width, height)
    update_title()

    def on_press(event):
        if event.xdata is None or event.ydata is None:
            return
        idx = nearest_line(event.x, event.y)
        if idx is not None:
            state["selected"] = idx
        elif state["selected"] is not None:
            line = lines[state["selected"]]
            if line["orientation"] == "h":
                line["position"] = min(max(event.ydata / height, 0.0), 1.0)
            else:
                line["position"] = min(max(event.xdata / width, 0.0), 1.0)
        redraw(ax, lines, width, height, state["selected"])
        update_title()

    def on_key(event):
        if event.key == "h":
            lines.append({"orientation": "h", "position": 0.5})
            state["selected"] = len(lines) - 1
        elif event.key == "v":
            lines.append({"orientation": "v", "position": 0.5})
            state["selected"] = len(lines) - 1
        elif event.key == "u" and lines:
            lines.pop()
            state["selected"] = None
        elif event.key == "d" and state["selected"] is not None:
            lines.pop(state["selected"])
            state["selected"] = None
        else:
            return
        redraw(ax, lines, width, height, state["selected"])
        update_title()

    fig.canvas.mpl_connect("button_press_event", on_press)
    fig.canvas.mpl_connect("key_press_event", on_key)
    plt.show()
    return lines


storage_existing = load_json(STORAGE_PATH)
storage_results = dict(storage_existing)

for path in image_paths:
    print(f"\n--- {path.name} ---")

    grid_lines = annotate_image(
        path,
        storage_existing.get(path.name, []),
        title_suffix=" | shelf grid"
    )
    if grid_lines:
        storage_results[path.name] = grid_lines
        print(f"  {len(grid_lines)} line(s) saved")

save_json(STORAGE_PATH, storage_results)

print(f"\nground_truth.json saved ({len(storage_results)} image(s))")
