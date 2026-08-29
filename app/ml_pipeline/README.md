# CardioNeuro AI — Genuine ML/DL Pipeline Workspace

This folder hosts the offline machine learning & deep learning pipeline for processing **real patient datasets**, conducting data QA, training genuine models, evaluating performance, and exporting optimized runtime assets (TFLite / ONNX) for Android deployment.

---

## 📁 Directory Architecture

```
ml_pipeline/
├── data/
│   ├── raw/
│   │   ├── clinical/    # Raw tabular clinical records (CSV / TSV / Parquet)
│   │   ├── mri/         # Raw neuroimaging scans (DICOM / NIfTI .nii.gz / PNG / JPG)
│   │   ├── eeg/         # Raw electroencephalogram signals (EDF / BrainVision / CSV / NPY)
│   │   └── ecg/         # Raw electrocardiogram telemetry (WFDB / PhysioNet / CSV / MAT)
│   ├── processed/       # Cleaned & standardized feature tensors / arrays
│   └── splits/          # Strict patient-level train / validation / test indices
├── data_qa/
│   └── inspect_dataset.py  # Automated script for data integrity, missing values, & leakage audit
├── preprocessing/
│   └── preprocess_template.py # Feature normalization, signal filtering, image resizing
├── training/
│   └── train_template.py    # Patient-stratified training, loss tracking, checkpointing
├── evaluation/
│   └── evaluate_metrics.py  # Calculates Sensitivity, Specificity, F1, AUROC, Confusion Matrix
└── exported_models/        # Final converted TFLite (.tflite) or ONNX (.onnx) models
```

---

## 🔄 End-to-End Execution Workflow

```
┌─────────────┐     ┌─────────────┐     ┌────────────────┐     ┌──────────────────────┐
│  RAW DATA   │ ──> │   DATA QA   │ ──> │ PREPROCESSING  │ ──> │ PATIENT-LEVEL SPLIT  │
│  (Uploaded) │     │ (Inspection)│     │ (Clean/Format) │     │  (No Data Leakage)   │
└─────────────┘     └─────────────┘     └────────────────┘     └──────────────────────┘
                                                                           │
┌─────────────────┐     ┌─────────────┐     ┌────────────────┐             ▼
│ ANDROID RUNTIME │ <── │ MODEL EXPORT│ <── │ REAL METRICS   │ <── ┌──────────────────────┐
│ (ONNX/TFLite)   │     │(TFLite/ONNX)│     │(Sens/Spec/AUC) │     │ TRAINING & VALIDATION│
└─────────────────┘     └─────────────┘     └────────────────┘     └──────────────────────┘
```

---

## 📋 Dataset Inspection Protocol

Upon uploading any raw dataset file (clinical, MRI, EEG, or ECG), the inspection script will automatically generate a report covering:
1. **File Format & Structure**: Validating schema, headers, binary encodings.
2. **Record & Patient Count**: Distinguishing unique patient IDs vs repeated/longitudinal recordings.
3. **Features & Modality Specs**: Signal sampling frequency, image dimensions/channels, clinical variable types.
4. **Target Labels & Class Imbalance**: Exact positive/negative distribution.
5. **Data Quality & Integrity**: Identifying null/missing values, corrupted headers, or signal artifacts.
6. **Patient-Level Leakage Audit**: Guaranteeing no cross-talk between patient instances across splits.
7. **Task Determination**: Formulating legitimate, evidence-based prediction objectives for the specific dataset.

---

## 🚫 Strict Governance Rules

1. **No Data Fabrication**: Zero synthetic record generation, zero label interpolation across unrelated datasets.
2. **Isolated Patient Pipelines**: Patients from independent sources remain decoupled unless explicitly linked by identical patient IDs.
3. **Locked Test Set**: The test set is evaluated exactly once after training/validation hyperparameter tuning is finalized.
4. **Unbiased Metrics**: Performance is strictly reported using Sensitivity, Specificity, F1-Score, AUROC, and Confusion Matrices on the untouched test set.
5. **No Fake Accuracy Claims**: The Android app will display "NOT IMPLEMENTED / UNTRAINED" until real trained model binaries are successfully exported and validated.
