"""
For parsing the HYG database for named stars and desired data.
"""
import csv
import json

big_dipper_stars = {"proper", "Alkaid", "Mizar", "Alioth", "Megrez", "Phecda", "Merak", "Dubhe"}

# Parse csv
csv_name = str(input("csv name: "))
with open(csv_name, encoding="utf-8") as source:
    reader = csv.reader(source)
    with open(f"parsed_{csv_name}", "w", encoding="utf-8") as parsed:
        writer = csv.writer(parsed, lineterminator="")
        for row in reader:
            if row[6] == "Sol": #or row[6] not in big_dipper_stars:
                continue
            writer.writerow((row[6], row[17], row[18], row[19]))
            parsed.write("\n")

# Convert to json
jsonArray = []
with open(f"parsed_{csv_name}", encoding='utf-8') as parsed:
    csvReader = csv.DictReader(parsed)
    for row in csvReader:
        jsonArray.append(row)

with open(csv_name.replace(".csv", ".json"), 'w', encoding='utf-8') as jsonf:
    jsonString = json.dumps(jsonArray, indent=4)
    jsonf.write(jsonString.replace("proper", "name"))
