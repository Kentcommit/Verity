from pathlib import Path
from urllib.parse import urlparse
import ipaddress
import re
import struct
import string

import numpy as np
import pandas as pd
from sklearn.ensemble import ExtraTreesClassifier
from sklearn.model_selection import train_test_split

DATASET_PATH = Path(r"C:\Users\mohan\Downloads\malicious_phish.csv")
OUTPUT_PATH = Path("app/src/main/assets/malicious_url_model.bin")
RANDOM_STATE = 42
FEATURE_NAMES = [
    "url_length", "host_length", "path_length", "digit_count", "letter_count",
    "special_count", "dot_count", "hyphen_count", "slash_count", "at_count",
    "percent_count", "has_https", "has_ip_address", "has_suspicious_term",
    "has_login_path", "has_punycode",
]
TERMS = re.compile(r"login|signin|verify|account|secure|update|password|confirm|bank|wallet|payment|free|bonus|crypto", re.IGNORECASE)
LOGIN_PATH = re.compile(r"login|signin|account|verify|secure", re.IGNORECASE)


def parse_url(value):
    try:
        return urlparse(value if "://" in value else "http://" + value)
    except ValueError:
        return urlparse("")


def has_ip_address(value):
    try:
        ipaddress.ip_address(parse_url(value).hostname or "")
        return 1
    except ValueError:
        return 0


def url_features(value):
    parsed = parse_url(value)
    host = parsed.hostname or ""
    return [
        len(value),
        len(host),
        len(parsed.path + "?" + parsed.query),
        sum(character.isdigit() for character in value),
        sum(character.isalpha() for character in value),
        sum(character in string.punctuation for character in value),
        value.count("."),
        value.count("-"),
        value.count("/"),
        value.count("@"),
        value.count("%"),
        int(parsed.scheme.lower() == "https"),
        has_ip_address(value),
        int(bool(TERMS.search(value))),
        int(bool(LOGIN_PATH.search(parsed.path))),
        int("xn--" in host.lower()),
    ]


def write_model(model):
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT_PATH.open("wb") as output:
        output.write(b"VURL1")
        output.write(struct.pack("<III", len(FEATURE_NAMES), len(model.estimators_), len(model.classes_)))
        for label in model.classes_:
            encoded = str(label).encode("utf-8")
            output.write(struct.pack("<I", len(encoded)))
            output.write(encoded)
        for estimator in model.estimators_:
            tree = estimator.tree_
            node_count = tree.node_count
            output.write(struct.pack("<I", node_count))
            for node in range(node_count):
                output.write(struct.pack(
                    "<iiifi",
                    int(tree.children_left[node]),
                    int(tree.children_right[node]),
                    int(tree.feature[node]),
                    float(tree.threshold[node]),
                    int(np.argmax(tree.value[node][0])),
                ))


def main():
    data = pd.read_csv(DATASET_PATH)[["url", "type"]].dropna().drop_duplicates()
    data["url"] = data["url"].astype(str)
    features = pd.DataFrame([url_features(url) for url in data["url"]], columns=FEATURE_NAMES, index=data.index)
    labels = data["type"]
    if len(features) > 150_000:
        per_class = min(150_000 // labels.nunique(), int(labels.value_counts().min()))
        indices = np.concatenate([
            group.sample(n=per_class, random_state=RANDOM_STATE).index.to_numpy()
            for _, group in labels.groupby(labels)
        ])
        features = features.loc[indices]
        labels = labels.loc[indices]
    x_train, _, y_train, _ = train_test_split(
        features, labels, test_size=0.20, random_state=RANDOM_STATE, stratify=labels
    )
    model = ExtraTreesClassifier(
        n_estimators=250,
        random_state=RANDOM_STATE,
        n_jobs=-1,
        class_weight="balanced",
    )
    model.fit(x_train, y_train)
    write_model(model)
    print(f"Exported {len(model.estimators_)} trees and {len(FEATURE_NAMES)} features to {OUTPUT_PATH}")
    print(f"Classes: {list(model.classes_)}")
    print(f"Size: {OUTPUT_PATH.stat().st_size:,} bytes")


if __name__ == "__main__":
    main()
