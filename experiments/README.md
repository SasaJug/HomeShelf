# Experiments

Python-side exploratory work and evaluation for the shelf-comparison pipeline, kept
separate from the Android app.

Each subfolder is a self-contained experiment or visualization (e.g. `01_vlm_roi/`) and keeps
the data it uses next to its notebook. `requirements.txt` is shared, since all notebooks use the
same environment.

## Setup

```powershell
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```
