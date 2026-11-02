# L12 Maps and sets: looking things up

## `Map<K, V>`: key -> value lookup

A map stores **pairs**. Each key appears once and leads to one value, like a table in a code
of practice: land-cover class -> curve number.

```java
Map<String, Integer> cn = new HashMap<>();
cn.put("Woods", 73);                 // add or replace
int v = cn.get("Woods");             // look up (null if the key is absent)
cn.getOrDefault("Water", 98);        // value or a default
cn.containsKey("Woods");             // true
for (Map.Entry<String, Integer> e : cn.entrySet()) { e.getKey(); e.getValue(); }
```

| Implementation | Iteration order | Use when |
|----------------|-----------------|----------|
| `HashMap` | unspecified (do not rely on it) | fastest general lookup |
| `LinkedHashMap` | insertion order | output must follow input order |
| `TreeMap` | sorted by key | you need keys in order or ranges |

## `Set<E>`: unique items

A set keeps each element once: `HashSet`, `LinkedHashSet` (insertion order), `TreeSet` (sorted).
`add` returns `false` if the element was already present.

## Program walkthrough

The program counts land-cover classes along a transect of grid cells, looks up a curve number for
each class (TR-55 values for hydrologic soil group C, as used by the lab's catchment model),
computes the area-weighted curve number, and lists the distinct classes with a `TreeSet`.
`LinkedHashMap` and `TreeMap` are used so that the printed order is reproducible.

## Try it

1. Count how many times each word occurs in a sentence.
2. Use a `TreeMap<Double, String>` to find the soil layer at a given depth with `floorEntry`.
