# changes — documentazione spec-driven

Una cartella per change, numerata, condivisa a livello monorepo:

```
changes/
└── NNN-brief-description/
    ├── requirements.md          # cosa e perché — approvato PRIMA di implementare
    └── implementation-log.md    # cosa è stato fatto, test, decisioni
```

Generata dalla skill `/new-change`. Ogni change segue gate espliciti: clarification →
requirements review → commit consent → merge consent. Una change può toccare più aree del
monorepo (`infra/`, `frontend/`, `services/<app>/`) in un singolo commit atomico.
