#!/usr/bin/env bash
for i in {0..30}; do
    file="data/questions-$i.json"
    curl -S -s "https://opentdb.com/api.php?amount=50&category=$i" | jq '.results' > "$file"
    if grep -q "\[\]" "$file"; then
        rm "$file"
    fi
done
