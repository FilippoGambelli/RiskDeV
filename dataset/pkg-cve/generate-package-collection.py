import json
import re

# --- CONFIGURATION ---
INPUT_FILE = "allInfoPackages.json"
VULN_DB_FILE = "vulnerability.json"
OUTPUT_FILE = "package.json"

# Configurazione per la normalizzazione
MAX_VERSION_LEN = 6  # Lunghezza fissa dell'array (Major.Minor.Patch.PreType.PreNum.Dev)

# Mappatura per convertire stringhe (alpha, beta, etc) in numeri per l'ordinamento
# Valori negativi < 0 (release finale) < Valori positivi (post release)
VERSION_WEIGHTS = {
    'dev': -4,
    'a': -3, 'alpha': -3,
    'b': -2, 'beta': -2,
    'rc': -1, 'c': -1, 'pre': -1,
    'post': 1, 'pl': 1,  # Patch level / post
}

def load_json(filename):
    try:
        with open(filename, 'r', encoding='utf-8') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"Error: '{filename}' not found.")
        return None
    except json.JSONDecodeError:
        print(f"Error: '{filename}' is not a valid JSON.")
        return None

def save_json(data, filename):
    try:
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=4)
        print(f"Success: Created '{filename}' with {len(data)} entries.")
    except Exception as e:
        print(f"Error saving {filename}: {e}")

def normalize_version(version_str):
    """
    Trasforma una stringa di versione in un array DI SOLI INTERI a lunghezza fissa.
    Gestisce: 4.1a1, 2.0.1, 1.0rc1, ecc.
    
    Esempio logica (MAX_LEN=6):
    '2.0'     -> [2, 0, 0, 0, 0, 0]
    '2.0.1'   -> [2, 0, 1, 0, 0, 0]
    '4.1a1'   -> [4, 1, 0, -3, 1, 0]  (dove 'a' diventa -3)
    '1.0rc2'  -> [1, 0, 0, -1, 2, 0]  (dove 'rc' diventa -1)
    """
    if not version_str:
        return [0] * MAX_VERSION_LEN
    
    # 1. Scomposizione (Regex trova numeri o parole)
    v = str(version_str).lower()
    parts = re.findall(r'(\d+|[a-z]+)', v)
    
    normalized = []
    
    for part in parts:
        if part.isdigit():
            normalized.append(int(part))
        else:
            # Se è testo, cerca il peso nella mappa, altrimenti usa un valore basso di default (-5)
            weight = VERSION_WEIGHTS.get(part, -5)
            normalized.append(weight)
            
    # 2. Riempimento (Padding) con 0 fino a raggiungere la lunghezza fissa
    # Usiamo 0 perché in questo sistema '0' rappresenta la "neutralità" o la "release finale"
    # rispetto ai numeri negativi delle alpha/beta.
    while len(normalized) < MAX_VERSION_LEN:
        normalized.append(0)
        
    # 3. Troncatura (se per assurdo la versione fosse più lunga di MAX_LEN)
    return normalized[:MAX_VERSION_LEN]

def main():
    raw_packages = load_json(INPUT_FILE)
    vuln_db = load_json(VULN_DB_FILE)

    if raw_packages is None:
        return

    # Build CVE lookup table
    vuln_map = {}
    if vuln_db:
        for item in vuln_db:
            cve = item.get('cve_id')
            score = item.get('metrics', {}).get('baseScore', 0.0)
            if cve:
                vuln_map[cve] = float(score)

    final_results = []

    for pkg in raw_packages:
        package_id = pkg.get("_id")
        
        for ver in pkg.get("versions_detailed", []):
            raw_vulns = ver.get("vulnerabilities") or []
            
            # Risk Score Logic
            scores = []
            seen_cves = set()
            for v in raw_vulns:
                cve_id = v.get('cve_id')
                if cve_id and cve_id not in seen_cves:
                    if cve_id in vuln_map:
                        scores.append(vuln_map[cve_id])
                    seen_cves.add(cve_id)

            max_risk_score = round(max(scores), 1) if scores else 0.0

            # --- NUOVA LOGICA VERSIONE ---
            version_str = ver.get("version")
            # Genera array di lunghezza fissa (es. 6 interi)
            version_sort_array = normalize_version(version_str)

            entry = {
                "package_name": package_id,
                "version": version_str,
                "version_array": version_sort_array,  # Array coerente per ordinamento
                "author": pkg.get("author"),
                "author_email": pkg.get("author_email"),
                "description": pkg.get("description"),
                "package_url": pkg.get("package_url"),
                "documentation": pkg.get("Documentation"),
                "upload_time": ver.get("upload_time"),
                "requires_dist": ver.get("requires_dist"),
                "requires_python": ver.get("requires_python"),
                "vulnerabilities": raw_vulns,
                "risk_score": max_risk_score
            }
            final_results.append(entry)

    save_json(final_results, OUTPUT_FILE)

if __name__ == "__main__":
    main()