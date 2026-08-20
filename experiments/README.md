# Experiments

Python-side exploratory work and evaluation for the shelf-comparison pipeline, kept
separate from the Android app.

Each subfolder is a self-contained experiment or visualization (e.g. `01_vlm_roi/`). `device_data/`
and `requirements.txt` are shared, since multiple notebooks read
the same captured data with the same environment.

## Setup

```powershell
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

## `device_data/`

A local mirror of the `capture` build variant's on-device storage
(`Documents/HomeShelf` on the phone), pulled via `adb`. It's the input data for
notebooks that evaluate the comparison pipeline: reference photos, marked items,
golden "after" captures with ground truth, and the JSON reports produced by
`ComparisonPipelineGoldenTest`.

```
device_data/
├── storages/<storageId>/latest/    # reference photo, guide lines, marked items
├── golden/comparisons/<name>/      # golden "after" captures + ground truth
└── test_results/                   # pipeline_test_*.json reports from the golden test
    └── aligned/<goldenName>.jpg    # homography-aligned "after" photo per golden, latest run only
```

`test_results/aligned/` is what marked-item bounding boxes are actually valid against —
they're stored as fractions of the reference photo, and alignment is what puts each new
capture into that same coordinate frame. The raw `golden/comparisons/<name>/photo.jpg` is
pre-alignment, so boxes drawn on it are only approximately right.

`grids/`, `pending/`, and `shopping_list.json` are pulled too but gitignored —
they're either leftovers from older/unused code paths or transient/unrelated data,
not something any notebook here depends on.

### Updating it

Run the golden test on-device first (`ComparisonPipelineGoldenTest`) to refresh
`test_results/`, then re-pull everything from Android Studio's **Terminal** tab
(defaults to `cmd.exe`, hence the batch syntax below). The pull always wipes
`device_data/` first so it never nests into a duplicate folder on repeat runs:

```bat
if exist device_data rmdir /s /q device_data
adb pull /sdcard/Documents/HomeShelf device_data
```

Requires USB debugging enabled and the device authorized (`adb devices` should list
it as `device`, not `unauthorized`).
