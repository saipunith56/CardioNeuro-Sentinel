# Android ONNX Integration Technical Contract

This document specifies the exact data types, mathematical transformations, array dimensions, and tensor layouts required to deploy the five trained ONNX models in the **CardioNeuro Sentinel** Android application. 

---

## 1. Modality Model Contracts

### 1.1 MRI Modality Stroke Classification Model (`ds1_mri_stroke_model.onnx`)
*   **Target Task**: Multiclass Stroke Type Classification (3 classes).
*   **Model Filename**: `ds1_mri_stroke_model.onnx`
*   **Model Source Size**: 6.1 MB (~1,520,339 parameters).
*   **Runtime Core**: MobileNetV3-Small.
*   **Input Tensor**:
    *   Name: `input`
    *   Data Type: `float32`
    *   Shape: `[1, 3, 128, 128]` (Batch x Channels x Height x Width)
*   **Output Tensor**:
    *   Name: `output`
    *   Data Type: `float32`
    *   Shape: `[1, 3]` (Raw logits)
*   **Mathematical Preprocessing Requirements**:
    1.  **Decoder**: Extract 2D axial slice from image format.
    2.  **Redimension**: Resize pixels using bilinear/bicubic interpolation to $128 \times 128$.
    3.  **Color Space Conversion**: Ensure RGB channel order (not BGR).
    4.  **Rescaling**: Map pixel values from `[0, 255]` to float interval `[0.0, 1.0]` ($X_{scaled} = X / 255.0$).
    5.  **Channel Normalization**: Apply ImageNet channel-wise standardization:
        *   $Mean = [\mu_R=0.485, \mu_G=0.456, \mu_B=0.406]$
        *   $StdDev = [\sigma_R=0.229, \sigma_G=0.224, \sigma_B=0.225]$
        *   $X_{normalized} = (X_{scaled} - Mean) / StdDev$
    6.  **Tensor Formatting**: Permute axes from HWC (Height, Width, Channels) to CHW (Channels, Height, Width) format: `[3, 128, 128]`. Add batch axis to yield `[1, 3, 128, 128]`.
*   **Output Semantics & Class Mapping**:
    *   `0`: Haemorrhagic
    *   `1`: Ischemic
    *   `2`: Normal
    *   *Probability Conversion*: Apply Softmax across logits:
        $$P(c) = \frac{e^{z_c}}{\sum_{j=0}^{2} e^{z_j}}$$

---

### 1.2 ECG Modality Diagnostic Model (`ds4_ecg_model.onnx`)
*   **Target Task**: Multilabel Arrhythmia/Diagnostic Classification (5 categories).
*   **Model Filename**: `ds4_ecg_model.onnx`
*   **Model Source Size**: 2.2 MB (~200,901 parameters).
*   **Runtime Core**: 1D ResNet.
*   **Input Tensor**:
    *   Name: `input`
    *   Data Type: `float32`
    *   Shape: `[1, 12, 1000]` (Batch x Channels/Leads x Time Steps)
*   **Output Tensor**:
    *   Name: `output`
    *   Data Type: `float32`
    *   Shape: `[1, 5]` (Raw logits)
*   **Mathematical Preprocessing Requirements**:
    1.  **Butterworth Bandpass Filter**: Apply 4th-order Butterworth bandpass filter with cutoffs at $0.5\text{ Hz}$ (low) and $40.0\text{ Hz}$ (high), calibrated for a $100\text{ Hz}$ sampling rate. 
        *   Must be computed per-lead along the temporal dimension ($N=1000$) using standard difference equations.
    2.  **Lead-Wise Temporal Normalization**: Apply temporal Z-score standardization per lead. For each lead $c \in [0, 11]$:
        $$\mu_c = \frac{1}{1000}\sum_{t=0}^{999} x_{c,t}, \quad \sigma_c = \sqrt{\frac{1}{1000}\sum_{t=0}^{999} (x_{c,t} - \mu_c)^2 + 10^{-8}}$$
        $$x'_{c,t} = \frac{x_{c,t} - \mu_c}{\sigma_c}$$
    3.  **Tensor Layout**: Pack into a float buffer matching `[1, 12, 1000]`.
*   **Output Semantics & Class Mappings**:
    *   `0`: NORM (Normal ECG)
    *   `1`: MI (Myocardial Infarction)
    *   `2`: STTC (ST/T Change)
    *   `3`: CD (Conduction Disturbance)
    *   `4`: HYP (Hypertrophy)
    *   *Probability Conversion*: Apply Sigmoid class-wise to get independent probabilities:
        $$P(c) = \frac{1}{1 + e^{-y_c}}$$

---

### 1.3 EEG Modality Seizure Model (`ds5_eeg_seizure_model.onnx`)
*   **Target Task**: Binary Seizure Classification.
*   **Model Filename**: `ds5_eeg_seizure_model.onnx`
*   **Model Source Size**: 163 KB (~101,313 parameters).
*   **Runtime Core**: 3-layer 1D CNN.
*   **Input Tensor**:
    *   Name: `input`
    *   Data Type: `float32`
    *   Shape: `[1, 23, 256]` (Batch x Channels x Time Steps)
*   **Output Tensor**:
    *   Name: `output`
    *   Data Type: `float32`
    *   Shape: `[1, 1]` (Logit)
*   **Mathematical Preprocessing Requirements**:
    1.  **Channel-Wise Z-Score**: Normalize using global training statistics:
        *   $\mu_c$ and $\sigma_c$ are fixed arrays of 23 elements, loaded from processing assets:
            *   **Mean ($\mu$)**: `[0.29829, 0.26730, 0.29766, 0.32642, 0.28410, 0.32961, 0.21109, 0.32766, 0.31055, 0.11848, 0.18899, 0.30113, 0.35994, 0.15705, 0.22076, 0.25271, 0.19385, 0.15494, 0.15041, 0.31397, 0.25077, 0.16050, 0.14065]`
            *   **Std Dev ($\sigma$)**: `[90.2488, 91.2393, 84.5947, 77.5168, 83.6079, 91.6644, 69.0968, 74.2762, 86.8616, 71.2658, 75.3138, 78.2029, 81.1533, 78.8102, 70.2203, 71.3060, 80.8982, 60.8862, 85.0036, 76.8205, 87.6042, 66.5744, 64.5862]`
        *   Standardize each element:
            $$x'_{c,t} = \frac{x_{c,t} - \mu_c}{\sigma_c}$$
    2.  **Tensor Layout**: Shape `[1, 23, 256]` float buffer.
*   **Output Semantics & Class Mapping**:
    *   `0`: Non-seizure
    *   `1`: Seizure
    *   *Probability Conversion*: Apply Sigmoid to single output logit:
        $$P(\text{Seizure}) = \frac{1}{1 + e^{-y}}$$

---

### 1.4 Clinical DS2 Heart Disease Model (`ds2_heart_model.onnx`)
*   **Target Task**: Tabular Heart Disease Classifier (Binary).
*   **Model Filename**: `ds2_heart_model.onnx`
*   **Model Source Size**: 10 KB (~12,993 parameters).
*   **Runtime Core**: Multi-Layer Perceptron (Tabular NN).
*   **Input Tensor**:
    *   Name: `input`
    *   Data Type: `float32`
    *   Shape: `[1, 13]` (1 Batch x 13 Numerical Features)
*   **Output Tensor**:
    *   Name: `output`
    *   Data Type: `float32`
    *   Shape: `[1]` (Logit)
*   **Feature Sequence (Index 0 to 12)**:
    1.  `age`: Age in years.
    2.  `sex`: Gender (1.0 = Male, 0.0 = Female).
    3.  `cp`: Chest pain type (1 = atypical angina; 2 = pain; 3 = non-anginal; 4 = asymptomatic).
    4.  `trestbps`: Resting blood pressure (in mmHg).
    5.  `chol`: Serum cholesterol (mg/dL).
    6.  `fbs`: Fasting blood sugar (> 120 mg/dL: 1.0 = true, 0.0 = false).
    7.  `restecg`: Resting ECG results (0 = normal, 1 = ST-T wave event, 2 = LV hypertrophy).
    8.  `thalach`: Maximum heart rate achieved.
    9.  `exang`: Exercise-induced angina (1.0 = yes, 0.0 = no).
    10. `oldpeak`: ST depression induced by exercise.
    11. `slope`: Peak exercise ST segment slope.
    12. `ca`: Number of major vessels colored by fluoroscopy (0-3).
    13. `thal`: Thalassemia defect type (3 = normal; 6 = fixed defect; 7 = reversible defect).
*   **Mathematical Preprocessing Requirements**:
    1.  **Imputation**: Replace missing/null entries with trained medians:
        `[55.0, 1.0, 4.0, 130.0, 223.0, 0.0, 0.0, 140.0, 0.0, 0.6, 2.0, 0.0, 6.0]`
    2.  **StandardScaler Scaling**: Apply Z-score transformation for each feature $i \in [0, 12]$ using parameters $\mu_i$ (mean) and $\sigma_i$ (scale):
        $$scaled_i = \frac{unscaled_i - \mu_i}{\sigma_i}$$
        *   **Mean ($\mu$)**: `[53.76087, 0.78261, 3.25000, 132.28882, 201.37112, 0.15373, 0.62267, 137.53106, 0.37112, 0.89891, 1.84317, 0.20807, 5.59161]`
        *   **Standard Deviation ($\sigma$)**: `[9.49560, 0.41247, 0.92979, 18.04542, 107.02480, 0.36069, 0.80691, 25.17474, 0.48310, 1.07594, 0.52745, 0.60036, 1.38627]`
    3.  **Tensor Layout**: Float buffer format of size `[1, 13]`.
*   **Output Semantics & Class Mapping**:
    *   `0`: Heart Disease Absent (num = 0)
    *   `1`: Heart Disease Present (num > 0)
    *   *Probability Conversion*: Apply Sigmoid to logit:
        $$P(\text{Heart Disease}) = \frac{1}{1 + e^{-y}}$$

---

### 1.5 Clinical DS3 Improved Stroke Model (`ds3_stroke_model_improved.onnx`)
*   **Target Task**: Tabular Stroke Classifier (Binary).
*   **Model Filename**: `ds3_stroke_model_improved.onnx`
*   **Model Source Size**: 60 KB (~14,145 parameters).
*   **Runtime Core**: Tabular MLP (with Focal Loss / Weighted BCE).
*   **Input Tensor**:
    *   Name: `input`
    *   Data Type: `float32`
    *   Shape: `[1, 22]` (1 Batch x 22 Concatenated Features)
*   **Output Tensor**:
    *   Name: `output`
    *   Data Type: `float32`
    *   Shape: `[1]` (Logit)
*   **Feature Sequence (Index 0 to 21)**:
    *   **Numerical Features (Indices 0–2)**:
        1.  `age`: Age in years.
        2.  `avg_glucose_level`: Average blood glucose level.
        3.  `bmi`: Body Mass Index.
    *   **Categorical One-Hot Encoded Features (Indices 3–21)**:
        *   `gender` (OHE size 2): `[Female, Male]` (Indices 3-4)
        *   `hypertension` (OHE size 2): `[0, 1]` (Indices 5-6)
        *   `heart_disease` (OHE size 2): `[0, 1]` (Indices 7-8)
        *   `ever_married` (OHE size 2): `[No, Yes]` (Indices 9-10)
        *   `work_type` (OHE size 5): `[Govt_job, Never_worked, Private, Self-employed, children]` (Indices 11-15)
        *   `Residence_type` (OHE size 2): `[Rural, Urban]` (Indices 16-17)
        *   `smoking_status` (OHE size 4): `[Unknown, formerly smoked, never smoked, smokes]` (Indices 18-21)
*   **Mathematical Preprocessing Requirements**:
    1.  **Numerical Imputation**: Missing values in numerical features must be filled with training medians:
        `[age=45.0, avg_glucose_level=91.695, bmi=27.9]`
    2.  **Numerical Normalization (StandardScaler)**: Apply Z-scoring to numerical features $i \in [0, 2]$:
        $$scaled_i = \frac{unscaled_i - \mu_i}{\sigma_i}$$
        *   **Numerical Means ($\mu$)**: `[43.25060, 105.46875, 28.75920]`
        *   **Numerical StdDevs ($\sigma$)**: `[22.68196, 44.36474, 7.69563]`
    3.  **One-Hot Encoding**: Convert categories to binary floats (`1.0f` where matched, `0.0f` elsewhere). Categories map sequentially in alphabetical/standard sorting order according to `ohe_cat` definition. Unknown categories must map to all-zeros (`0.0f`).
    4.  **Vector Concatenation**: Stack numerical normalized elements `[0..2]` followed by OHE float indicators `[3..21]` to create the final 22-element input vector of shape `[1, 22]`.
*   **Output Semantics & Class Mapping**:
    *   `0`: Stroke Absent
    *   `1`: Stroke Present
    *   *Confidence Conversion*: Apply Sigmoid to single logit:
        $$P(\text{Stroke}) = \frac{1}{1 + e^{-y}}$$
    *   *Decision Boundary*: Classify as Stroke Present (`1`) if and only if $P(\text{Stroke}) \ge 0.70$.

---

## 2. Android App Audit Findings

### 2.1 Integration Mismatches
Upon auditing the current Android UI (`NewDiagnosticScreen.kt`) and database state, the following integration mismatches are identified:

*   **ECG Lead Incompatibility**:
    *   *App Input*: Drag-and-drop csv upload (filename preset: `ecg_lead_ii_30s.csv`) and string selection presets like `"Atrial Fibrillation"`.
    *   *Model Expectation*: A multi-dimensional array mapping 12 full leads (`12 x 1000`) at 100 Hz. The app lacks logic to map single-lead CSV files or text presets to 12-channel raw signals.
*   **EEG montage Incompatibility**:
    *   *App Input*: EDF file loader (mock filename: `eeg_telemetry_ch12.edf`) and text presets.
    *   *Model Expectation*: A `[1, 23, 256]` segment matrix. The app has no EDF signal parser nor channel layout mapping to map 12-channel EDF telemetry into the 23-channel input montage expected by `ds5_eeg_seizure_model.onnx`.
*   **MRI format Incompatibility**:
    *   *App Input*: DICOM patient scan files (`brain_scan_dwi_048.dcm`).
    *   *Model Expectation*: RGB 2D image pixel grid (`128x128`). Android lacks a DICOM standard image-slice extractor.
*   **Tabular Data Scarcity**:
    *   Features required for DS2 (`cp`, `restecg`, `thalach`, `oldpeak`, etc.) and DS3 (`work_type`, `Residence_type`, `ever_married`, `smoking_status`) are not modeled within the UI input screens or Room database fields for Patients. Currently, the UI collects a very limited baseline.

### 2.2 Multimodal risk calculations
*   **Scientific Caveat**: The app calculates a `combinedRiskScore` by finding the maximum of heart risk and stroke risk, adding 5 as a modifier. This is a heuristic simplification. Combining outputs does not represent a mathematically validated multimodal risk.
*   **Requirement**: In the final implementation, the UI must list separate diagnostic risks from individual neural networks next to the overall synthesized clinical score, rather than implying a single consolidated deep learning calculation package.

---

## 3. Scientific Limitations & Warnings
*   **EEG Leakage Warnings**: Validation metrics ($93.7\%$) on the EEG modality may be artificially inflated due to the lack of patient/subject indexing inside training splits.
*   **MRI Sample Size Warning**: MRI model was trained on slices from only 16 patients (Train: 10, Val: 3, Test: 3). Although slice-level accuracy is logged, patient-level generalization is extremely volatile and clinically untested.
*   **Clinical Stroke Imbalance**: The DS3 stroke model operates on a heavily imbalanced raw dataset (~5% stroke rate). Although sensitivity is high ($68\%$), precision is low ($15.3\%$) under the optimal threshold ($0.70$). False alarms are frequent.
