#!/usr/bin/env python3
"""Validates projects/projects.json against projects.schema.json (standard library only).

Checks the subset of JSON Schema used by the directory: required, additionalProperties, type,
pattern, enum, minLength, maxLength, maxItems, plus unique ids. Exit status 1 on any error.
"""
import json
import re
import sys


def check(value, schema, path, errors):
    t = schema.get("type")
    types = {"object": dict, "array": list, "string": str}
    if t in types and not isinstance(value, types[t]):
        errors.append(f"{path}: expected {t}")
        return
    if t == "object":
        for key in schema.get("required", []):
            if key not in value:
                errors.append(f"{path}: missing '{key}'")
        props = schema.get("properties", {})
        for key, v in value.items():
            if key in props:
                check(v, props[key], f"{path}.{key}", errors)
            elif schema.get("additionalProperties") is False:
                errors.append(f"{path}: unknown field '{key}'")
    elif t == "array":
        if "maxItems" in schema and len(value) > schema["maxItems"]:
            errors.append(f"{path}: more than {schema['maxItems']} items")
        for i, item in enumerate(value):
            check(item, schema.get("items", {}), f"{path}[{i}]", errors)
    elif t == "string":
        if "minLength" in schema and len(value) < schema["minLength"]:
            errors.append(f"{path}: shorter than {schema['minLength']} characters")
        if "maxLength" in schema and len(value) > schema["maxLength"]:
            errors.append(f"{path}: longer than {schema['maxLength']} characters")
        if "pattern" in schema and not re.search(schema["pattern"], value):
            errors.append(f"{path}: '{value}' does not match {schema['pattern']}")
        if "enum" in schema and value not in schema["enum"]:
            errors.append(f"{path}: '{value}' is not one of {schema['enum']}")


def validate(data, schema):
    """Returns a list of error messages (empty when valid)."""
    errors = []
    check(data, schema, "$", errors)
    ids = [p.get("id") for p in data.get("projects", []) if isinstance(p, dict)]
    for dup in sorted({i for i in ids if ids.count(i) > 1}):
        errors.append(f"$.projects: duplicate id '{dup}'")
    return errors


def main(data_path="projects/projects.json", schema_path="projects/projects.schema.json"):
    data = json.load(open(data_path, encoding="utf-8"))
    schema = json.load(open(schema_path, encoding="utf-8"))
    errors = validate(data, schema)
    for e in errors:
        print("ERROR", e)
    print(f"{len(data.get('projects', []))} projects checked, {len(errors)} errors")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(*sys.argv[1:]))
