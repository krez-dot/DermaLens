# Acne subtype differential — deferred

Idea: instead of one generic "Acne Vulgaris" class, split acne detection into subtypes
(blackhead / whitehead / papula / nodules[/pustula]) so the app can tell the user which kind
of acne it's seeing, not just "Acne."

**Deferred, not started** — decided to keep training on a single merged "Acne Vulgaris" class for
now (see `merge_and_train_multiclass.ipynb`) rather than expand the model to ~10 classes while the
current 6-class model is still below its 0.70 mAP target. Revisit only after the 6-class model
hits target.

## Why this needs real thought before starting, not just more training

- Expands the model from 6 classes to ~10. More visually-similar classes historically hurts overall
  mAP, not helps it — exactly the failure mode already seen with Scabies in the 6-class run.
- Thin per-subtype sample sizes: pustula only has 70 instances, nodules 154 — both well below even
  the current weakest class (Melasma, 608 images).
- Not just a training change: `DetectionResult` / `mockDetectionResults` / Care Guide content in
  `ScanResultScreen.kt` is built around one condition = one result screen. Subtype output needs new
  content per subtype, not just new training labels.

## Candidate dataset (found, not yet merged into the pipeline)

**acne-skin** by lulu — Roboflow Universe
- `rf.workspace("lulu-vv2dw").project("acne-skin-n7ajw")`, version 1
- Object-detection (real bounding boxes, not polygon) ✅
- Already exported with "Stretch to" 640x640 resize ✅ — no resize fix needed unlike Melasma/Scabies
- License: CC BY 4.0 (permissive, attribution required)
- 630 images (560 train / 35 valid / 35 test)
- 5 classes, heavily skewed toward blackhead:
  - blackhead: 2,588 instances
  - whitehead: 380
  - papula: 265
  - nodules: 154
  - pustula: 70

Ruled out: **pimples** by first-project1 (`first-project1/pimples-z7dpw`) — instance-segmentation
(polygon annotations get silently dropped by the notebook's bounding-box export), plus 3 stray
zero-count junk classes (`0`, `1`, `5`) that would fail the preflight single-class check anyway even
if the segmentation problem were fixed.

## If/when this gets picked up

1. Decide the app-side design first (Care Guide content, DetectionResult entries, ScanResultScreen
   UI) before touching training — the model change is the easy part.
2. Merge the 6-class model's own "Acne Vulgaris" data (`acne-fixed`) with acne-skin's per-subtype
   labels kept separate (don't merge acne-skin's 5 classes into 1 this time).
3. Expect a real mAP hit on the acne subtypes specifically given the sample sizes above — budget
   time for an annotation-quality pass up front rather than treating it as an afterthought, same
   lesson as Scabies.
