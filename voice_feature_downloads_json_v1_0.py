#!/usr/bin/env python3
"""
VOICE FEATURE DOWNLOADS JSON V1.0

Takes ONE audio/video file from the user's Downloads folder and creates a
JSON file containing:
  - input file metadata
  - duration
  - number of analysis windows
  - complete V12 acoustic features for each window
  - expanded V14.2 pitch/spectral/MFCC/energy/rhythm features
  - aggregate robust statistics for every numeric feature

Default behavior:
  1. Look in ~/Downloads
  2. Select the newest supported audio/video file
  3. Write <input_stem>_voice_features.json next to the input file

You can explicitly pass a file:
  python voice_feature_downloads_json_v1_0.py "C:\\Users\\you\\Downloads\\sample.mp3"

You can choose an output JSON path:
  python voice_feature_downloads_json_v1_0.py "..." --output "C:\\...\\result.json"

This is an ANALYZER only.
It does NOT compare speakers, train an anti-spoof detector, or modify any
production speaker model.

The feature extraction logic is based on the supplied V14.2 feature-expansion
code and its V12 acoustic extractor.
"""

from __future__ import annotations

import argparse
import json
import math
import shutil
import tempfile
from datetime import datetime, timezone
from pathlib import Path

import librosa
import numpy as np

import voice_fingerprint_v12_0 as v12


SUPPORTED_EXTS = getattr(
    v12,
    "AUDIO_EXTS",
    {
        ".wav",
        ".mp3",
        ".m4a",
        ".mp4",
        ".mpeg",
        ".mpga",
        ".ogg",
        ".flac",
        ".aac",
        ".webm",
    },
)


def finite(value) -> bool:
    try:
        return math.isfinite(float(value))
    except Exception:
        return False


def json_safe(value):
    """Convert numpy/scalar containers into JSON-safe Python values."""
    if value is None:
        return None

    if isinstance(
        value,
        (str, int, bool),
    ):
        return value

    if isinstance(
        value,
        (np.integer,),
    ):
        return int(value)

    if isinstance(
        value,
        (np.floating,),
    ):
        value = float(value)

    if isinstance(
        value,
        (float,),
    ):
        return value if math.isfinite(value) else None

    if isinstance(
        value,
        np.ndarray,
    ):
        return [
            json_safe(x)
            for x in value.tolist()
        ]

    if isinstance(
        value,
        dict,
    ):
        return {
            str(k): json_safe(v)
            for k, v in value.items()
        }

    if isinstance(
        value,
        (list, tuple),
    ):
        return [
            json_safe(x)
            for x in value
        ]

    return str(value)


def robust_stats(values):
    x = np.asarray(
        [
            float(v)
            for v in values
            if finite(v)
        ],
        dtype=float,
    )

    if x.size == 0:
        return None

    q10, q25, q50, q75, q90 = np.percentile(
        x,
        [10, 25, 50, 75, 90],
    )

    mean = float(np.mean(x))
    std = float(np.std(x))

    if std > 1e-12:
        centered = (
            x - mean
        ) / std
        skew = float(
            np.mean(
                centered ** 3
            )
        )
        kurtosis = float(
            np.mean(
                centered ** 4
            ) - 3.0
        )
    else:
        skew = 0.0
        kurtosis = 0.0

    return {
        "n": int(x.size),
        "q10": float(q10),
        "q25": float(q25),
        "q50": float(q50),
        "q75": float(q75),
        "q90": float(q90),
        "iqr": float(
            max(
                q75 - q25,
                1e-12,
            )
        ),
        "mean": mean,
        "std": std,
        "skew": skew,
        "kurtosis": kurtosis,
        "min": float(np.min(x)),
        "max": float(np.max(x)),
        "range": float(
            np.max(x) - np.min(x)
        ),
    }


def make_extra_features(
    y,
    sr,
):
    """
    Port of the supplied V14.2 expanded feature function.
    """
    y = np.asarray(
        y,
        dtype=np.float32,
    )

    out = {}

    # ---------------- PITCH ----------------
    f0 = librosa.yin(
        y,
        fmin=55.0,
        fmax=min(
            600.0,
            sr / 2.0 - 50.0,
        ),
        sr=sr,
        frame_length=2048,
        hop_length=256,
    )

    f0 = f0[
        np.isfinite(f0)
    ]

    if f0.size:
        fs = robust_stats(f0)

        out[
            "new.pitch.f0_q10"
        ] = fs["q10"]
        out[
            "new.pitch.f0_q25"
        ] = fs["q25"]
        out[
            "new.pitch.f0_median"
        ] = fs["q50"]
        out[
            "new.pitch.f0_q75"
        ] = fs["q75"]
        out[
            "new.pitch.f0_q90"
        ] = fs["q90"]
        out[
            "new.pitch.f0_iqr"
        ] = fs["iqr"]
        out[
            "new.pitch.f0_range"
        ] = (
            fs["q90"]
            - fs["q10"]
        )
        out[
            "new.pitch.f0_std"
        ] = fs["std"]
        out[
            "new.pitch.f0_skew"
        ] = fs["skew"]
        out[
            "new.pitch.f0_kurtosis"
        ] = fs["kurtosis"]

        d1 = np.diff(f0)
        d2 = np.diff(
            f0,
            n=2,
        )
        turns = np.diff(
            np.sign(d1)
        )

        out[
            "new.pitch.pitch_slope_std"
        ] = (
            float(np.std(d1))
            if d1.size
            else None
        )

        out[
            "new.pitch.pitch_accel_std"
        ] = (
            float(np.std(d2))
            if d2.size
            else None
        )

        out[
            "new.pitch.pitch_turn_rate"
        ] = (
            float(
                np.mean(
                    turns != 0
                )
            )
            if turns.size
            else 0.0
        )
    else:
        for key in [
            "new.pitch.f0_q10",
            "new.pitch.f0_q25",
            "new.pitch.f0_median",
            "new.pitch.f0_q75",
            "new.pitch.f0_q90",
            "new.pitch.f0_iqr",
            "new.pitch.f0_range",
            "new.pitch.f0_std",
            "new.pitch.f0_skew",
            "new.pitch.f0_kurtosis",
            "new.pitch.pitch_slope_std",
            "new.pitch.pitch_accel_std",
            "new.pitch.pitch_turn_rate",
        ]:
            out[key] = None

    # ---------------- SPECTRAL ----------------
    S = (
        np.abs(
            librosa.stft(
                y,
                n_fft=2048,
                hop_length=256,
                win_length=2048,
            )
        )
        + 1e-12
    )

    freqs = librosa.fft_frequencies(
        sr=sr,
        n_fft=2048,
    )

    P = S / np.maximum(
        np.sum(
            S,
            axis=0,
            keepdims=True,
        ),
        1e-12,
    )

    entropy = -np.sum(
        P
        * np.log(
            P + 1e-12
        ),
        axis=0,
    )

    entropy /= math.log(
        max(
            S.shape[0],
            2,
        )
    )

    if S.shape[1] >= 2:
        flux = np.sqrt(
            np.sum(
                np.diff(
                    P,
                    axis=1,
                ) ** 2,
                axis=0,
            )
        )
    else:
        flux = np.zeros(
            1,
            dtype=np.float32,
        )

    centroid = (
        librosa.feature.spectral_centroid(
            S=S,
            sr=sr,
        ).ravel()
    )

    bandwidth = (
        librosa.feature.spectral_bandwidth(
            S=S,
            sr=sr,
        ).ravel()
    )

    rolloff = (
        librosa.feature.spectral_rolloff(
            S=S,
            sr=sr,
            roll_percent=0.85,
        ).ravel()
    )

    flatness = (
        librosa.feature.spectral_flatness(
            S=S
        ).ravel()
    )

    x0 = freqs - np.mean(
        freqs
    )

    den = (
        np.sum(
            x0 * x0
        )
        + 1e-12
    )

    slopes = np.asarray(
        [
            np.sum(
                x0
                * (
                    col
                    - np.mean(col)
                )
            )
            / den
            for col in S.T
        ],
        dtype=np.float32,
    )

    contrast = (
        librosa.feature.spectral_contrast(
            S=S,
            sr=sr,
            n_bands=3,
        )
    )

    cep = []

    logS = np.log(
        np.maximum(
            S,
            1e-12,
        )
    )

    for col in logS.T:
        c = (
            col
            - np.mean(col)
        )

        ac = np.correlate(
            c,
            c,
            mode="full",
        )[
            len(c) - 1:
        ]

        if ac.size > 4:
            cep.append(
                np.max(
                    ac[
                        2:min(
                            80,
                            ac.size,
                        )
                    ]
                )
                / max(
                    ac[0],
                    1e-12,
                )
            )

    out[
        "new.voice.spectral_entropy"
    ] = (
        float(
            np.mean(
                entropy
            )
        )
        if entropy.size
        else None
    )

    out[
        "new.voice.cepstral_peak_proxy"
    ] = (
        float(
            np.mean(
                cep
            )
        )
        if cep
        else None
    )

    out[
        "new.voice.hnr_low_proxy"
    ] = float(
        np.mean(
            contrast[0]
        )
    )

    out[
        "new.voice.hnr_mid_proxy"
    ] = float(
        np.mean(
            contrast[1]
        )
    )

    out[
        "new.voice.hnr_high_proxy"
    ] = float(
        np.mean(
            contrast[-1]
        )
    )

    out[
        "new.spectral.flux_mean"
    ] = (
        float(np.mean(flux))
        if flux.size
        else None
    )

    out[
        "new.spectral.flux_std"
    ] = (
        float(np.std(flux))
        if flux.size
        else None
    )

    out[
        "new.spectral.entropy_mean"
    ] = (
        float(
            np.mean(entropy)
        )
        if entropy.size
        else None
    )

    out[
        "new.spectral.entropy_std"
    ] = (
        float(
            np.std(entropy)
        )
        if entropy.size
        else None
    )

    for key, arr, q in [
        (
            "new.spectral.centroid_q10",
            centroid,
            "q10",
        ),
        (
            "new.spectral.centroid_q50",
            centroid,
            "q50",
        ),
        (
            "new.spectral.centroid_q90",
            centroid,
            "q90",
        ),
        (
            "new.spectral.bandwidth_q10",
            bandwidth,
            "q10",
        ),
        (
            "new.spectral.bandwidth_q50",
            bandwidth,
            "q50",
        ),
        (
            "new.spectral.bandwidth_q90",
            bandwidth,
            "q90",
        ),
        (
            "new.spectral.rolloff_q10",
            rolloff,
            "q10",
        ),
        (
            "new.spectral.rolloff_q50",
            rolloff,
            "q50",
        ),
        (
            "new.spectral.rolloff_q90",
            rolloff,
            "q90",
        ),
        (
            "new.spectral.flatness_q50",
            flatness,
            "q50",
        ),
        (
            "new.spectral.slope_q25",
            slopes,
            "q25",
        ),
        (
            "new.spectral.slope_q50",
            slopes,
            "q50",
        ),
        (
            "new.spectral.slope_q75",
            slopes,
            "q75",
        ),
    ]:
        st = robust_stats(
            arr
        )

        out[key] = (
            st[q]
            if st
            else None
        )

    # ---------------- MFCC ----------------
    mfcc = librosa.feature.mfcc(
        y=y,
        sr=sr,
        n_mfcc=20,
        n_fft=2048,
        hop_length=256,
    )

    delta = librosa.feature.delta(
        mfcc,
        width=9,
        mode="nearest",
    )

    for i in range(
        20
    ):
        st = robust_stats(
            mfcc[i]
        )

        out[
            f"new.mfcc.mfcc[{i}].std"
        ] = (
            st["std"]
            if st
            else None
        )

        out[
            f"new.mfcc.mfcc[{i}].iqr"
        ] = (
            st["iqr"]
            if st
            else None
        )

    for i in range(
        min(
            13,
            delta.shape[0],
        )
    ):
        st = robust_stats(
            delta[i]
        )

        out[
            f"new.mfcc.delta[{i}].std"
        ] = (
            st["std"]
            if st
            else None
        )

    # ---------------- ENERGY ----------------
    rms = librosa.feature.rms(
        y=y,
        frame_length=2048,
        hop_length=256,
    ).ravel()

    rms_db = (
        20.0
        * np.log10(
            np.maximum(
                rms,
                1e-8,
            )
        )
    )

    rs = robust_stats(
        rms
    )
    rdb = robust_stats(
        rms_db
    )

    out["new.energy.rms_q10"] = (
        rs["q10"]
        if rs
        else None
    )
    out["new.energy.rms_q25"] = (
        rs["q25"]
        if rs
        else None
    )
    out["new.energy.rms_q50"] = (
        rs["q50"]
        if rs
        else None
    )
    out["new.energy.rms_q75"] = (
        rs["q75"]
        if rs
        else None
    )
    out["new.energy.rms_q90"] = (
        rs["q90"]
        if rs
        else None
    )
    out["new.energy.rms_iqr"] = (
        rs["iqr"]
        if rs
        else None
    )

    out[
        "new.energy.rms_db_q10"
    ] = (
        rdb["q10"]
        if rdb
        else None
    )

    out[
        "new.energy.rms_db_q50"
    ] = (
        rdb["q50"]
        if rdb
        else None
    )

    out[
        "new.energy.rms_db_q90"
    ] = (
        rdb["q90"]
        if rdb
        else None
    )

    if rms.size >= 3:
        n = max(
            1,
            rms.size // 3,
        )

        out[
            "new.energy.attack_proxy"
        ] = float(
            np.mean(
                rms[n:2 * n]
            )
            - np.mean(
                rms[:n]
            )
        )

        out[
            "new.energy.release_proxy"
        ] = float(
            np.mean(
                rms[-n:]
            )
            - np.mean(
                rms[-2 * n:-n]
            )
        )
    else:
        out[
            "new.energy.attack_proxy"
        ] = 0.0

        out[
            "new.energy.release_proxy"
        ] = 0.0

    # ---------------- RHYTHM ----------------
    zcr = (
        librosa.feature.zero_crossing_rate(
            y,
            frame_length=2048,
            hop_length=256,
        ).ravel()
    )

    threshold = (
        np.percentile(
            rms,
            30,
        )
        if rms.size
        else 0.0
    )

    voiced = (
        rms
        > max(
            threshold,
            np.median(rms)
            * 0.5,
        )
        if rms.size
        else np.array(
            [],
            dtype=bool,
        )
    )

    def runs(mask):
        if mask.size == 0:
            return []

        result = []

        current = bool(
            mask[0]
        )
        start = 0

        for i in range(
            1,
            len(mask),
        ):
            bit = bool(
                mask[i]
            )

            if bit != current:
                result.append(
                    i - start
                )
                start = i
                current = bit

        result.append(
            len(mask)
            - start
        )

        return result

    frame_s = (
        256
        / float(sr)
    )

    voiced_runs = (
        np.asarray(
            runs(voiced),
            dtype=float,
        )
        * frame_s
    )

    unvoiced_runs = (
        np.asarray(
            runs(~voiced),
            dtype=float,
        )
        * frame_s
    )

    out[
        "new.rhythm.voiced_fraction"
    ] = (
        float(
            np.mean(voiced)
        )
        if voiced.size
        else None
    )

    out[
        "new.rhythm.voiced_run_median_s"
    ] = (
        float(
            np.median(
                voiced_runs
            )
        )
        if voiced_runs.size
        else None
    )

    out[
        "new.rhythm.voiced_run_iqr_s"
    ] = (
        float(
            np.percentile(
                voiced_runs,
                75,
            )
            - np.percentile(
                voiced_runs,
                25,
            )
        )
        if voiced_runs.size > 1
        else (
            0.0
            if voiced_runs.size
            else None
        )
    )

    out[
        "new.rhythm.unvoiced_run_median_s"
    ] = (
        float(
            np.median(
                unvoiced_runs
            )
        )
        if unvoiced_runs.size
        else None
    )

    out[
        "new.rhythm.unvoiced_run_iqr_s"
    ] = (
        float(
            np.percentile(
                unvoiced_runs,
                75,
            )
            - np.percentile(
                unvoiced_runs,
                25,
            )
        )
        if unvoiced_runs.size > 1
        else (
            0.0
            if unvoiced_runs.size
            else None
        )
    )

    out[
        "new.rhythm.zero_crossing_std"
    ] = (
        float(
            np.std(zcr)
        )
        if zcr.size
        else None
    )

    return out


def extract_windows(
    path,
):
    td = Path(
        tempfile.mkdtemp(
            prefix="voice_downloads_v1_"
        )
    )

    try:
        wav = v12.convert_to_temp_wav(
            path,
            td,
        )

        _, duration = (
            v12.wav_info(
                wav
            )
        )

        result = []

        for index, seg in enumerate(
            v12.iter_windows(
                wav,
                duration,
            ),
            start=1,
        ):
            try:
                y = seg[
                    "y"
                ]

                old = (
                    v12.extract_acoustic(
                        y,
                        v12.SAMPLE_RATE,
                    )
                )

                new = (
                    make_extra_features(
                        y,
                        v12.SAMPLE_RATE,
                    )
                )

                f1 = old.get(
                    "formants.F1"
                )
                f2 = old.get(
                    "formants.F2"
                )
                f3 = old.get(
                    "formants.F3"
                )

                new[
                    "new.formants.F2_minus_F1"
                ] = (
                    float(
                        f2 - f1
                    )
                    if finite(f1)
                    and finite(f2)
                    else None
                )

                new[
                    "new.formants.F3_minus_F2"
                ] = (
                    float(
                        f3 - f2
                    )
                    if finite(f2)
                    and finite(f3)
                    else None
                )

                new[
                    "new.formants.F2_F1_log_ratio"
                ] = (
                    float(
                        math.log(
                            max(
                                f2,
                                1e-6,
                            )
                            / max(
                                f1,
                                1e-6,
                            )
                        )
                    )
                    if finite(f1)
                    and finite(f2)
                    else None
                )

                new[
                    "new.formants.F3_F2_log_ratio"
                ] = (
                    float(
                        math.log(
                            max(
                                f3,
                                1e-6,
                            )
                            / max(
                                f2,
                                1e-6,
                            )
                        )
                    )
                    if finite(f2)
                    and finite(f3)
                    else None
                )

                merged = dict(
                    old
                )
                merged.update(
                    new
                )

                result.append({
                    "window_index": index,
                    "features": json_safe(
                        merged
                    ),
                })

            finally:
                try:
                    del seg["y"]
                except Exception:
                    pass

        return result, float(
            duration
        )

    finally:
        shutil.rmtree(
            td,
            ignore_errors=True,
        )


def aggregate_windows(
    windows,
):
    values_by_feature = {}

    for window in windows:
        for name, value in window[
            "features"
        ].items():
            if finite(value):
                values_by_feature.setdefault(
                    name,
                    [],
                ).append(
                    float(value)
                )

    aggregate = {}

    for name, values in sorted(
        values_by_feature.items()
    ):
        aggregate[name] = robust_stats(
            values
        )

    return aggregate


def pick_default_download():
    downloads = (
        Path.home()
        / "Downloads"
    )

    if not downloads.is_dir():
        raise FileNotFoundError(
            f"Downloads folder not found: {downloads}"
        )

    files = [
        p
        for p in downloads.iterdir()
        if (
            p.is_file()
            and p.suffix.lower()
            in SUPPORTED_EXTS
        )
    ]

    if not files:
        supported = ", ".join(
            sorted(
                SUPPORTED_EXTS
            )
        )
        raise FileNotFoundError(
            f"No supported audio/video files found in {downloads}. "
            f"Supported extensions: {supported}"
        )

    # Newest modified file is selected.
    return max(
        files,
        key=lambda p: p.stat().st_mtime,
    )


def main():
    parser = argparse.ArgumentParser(
        description=(
            "Analyze one audio/video file from Downloads "
            "and write a JSON feature report."
        )
    )

    parser.add_argument(
        "input_file",
        nargs="?",
        help=(
            "Audio/video file. If omitted, the newest supported "
            "file in ~/Downloads is used."
        ),
    )

    parser.add_argument(
        "--output",
        help=(
            "Output JSON path. Default: "
            "<input>_voice_features.json"
        ),
    )

    args = parser.parse_args()

    if args.input_file:
        input_path = Path(
            args.input_file
        ).expanduser()
    else:
        input_path = pick_default_download()

    if not input_path.is_file():
        raise FileNotFoundError(
            f"Input file not found: {input_path}"
        )

    print(
        "=" * 100
    )
    print(
        "VOICE FEATURE DOWNLOADS JSON V1.0"
    )
    print(
        "=" * 100
    )
    print(
        f"Input: {input_path}"
    )

    windows, duration = (
        extract_windows(
            input_path
        )
    )

    aggregate = (
        aggregate_windows(
            windows
        )
    )

    all_feature_names = sorted(
        {
            name
            for w in windows
            for name in w["features"]
        }
    )

    per_window_count = len(
        windows
    )

    payload = {
        "version": "1.0",
        "analysis_type": (
            "V12 acoustic + V14.2 expanded feature extraction"
        ),
        "production_model_modified": False,
        "input": {
            "path": str(
                input_path.resolve()
            ),
            "filename": input_path.name,
            "extension": input_path.suffix.lower(),
            "size_bytes": input_path.stat().st_size,
            "modified_time": datetime.fromtimestamp(
                input_path.stat().st_mtime,
                tz=timezone.utc,
            ).isoformat(),
        },
        "audio": {
            "duration_seconds": duration,
            "analysis_windows": per_window_count,
            "feature_count": len(
                all_feature_names
            ),
            "sample_rate": int(
                v12.SAMPLE_RATE
            ),
        },
        "features": {
            "names": all_feature_names,
            "aggregate": aggregate,
            "windows": windows,
        },
    }

    if args.output:
        output_path = Path(
            args.output
        ).expanduser()
    else:
        output_path = (
            input_path.parent
            / (
                input_path.stem
                + "_voice_features.json"
            )
        )

    output_path.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    output_path.write_text(
        json.dumps(
            payload,
            indent=2,
            allow_nan=False,
        ),
        encoding="utf-8",
    )

    print(
        f"Duration: {duration:.2f}s"
    )
    print(
        f"Analysis windows: {per_window_count}"
    )
    print(
        f"Feature count: {len(all_feature_names)}"
    )
    print(
        f"JSON: {output_path}"
    )
    print(
        "COMPLETE"
    )


if __name__ == "__main__":
    main()
