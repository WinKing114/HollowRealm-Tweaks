"""Run real, reobfuscated JARs in a new local dedicated server. Never installs into HollowRealm."""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess
import time

ROOT = Path(__file__).resolve().parent
SERVER = ROOT / "runtime-server"
JAVA = Path(r"C:\Program Files\Zulu\zulu-17\bin\java.exe")
INSTALLED_MODS = Path(r"C:\Users\WinKing\Documents\.minecraft\versions\HollowRealm\mods")
VERSION = "0.4.0"
EVIDENCE = ROOT / "evidence" / VERSION
TARGETS = json.loads((EVIDENCE / "runtime-targets.json").read_text("utf-8"))
ARTIFACT = f"HollowRealm-Tweaks-{VERSION}.jar"
PROBE = f"hollowrealm-tweaks-probe-{VERSION}.jar"

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def write_probe_configs(mode):
    cfg_dir = SERVER / "config"
    default_cfg_dir = SERVER / "defaultconfigs"
    cfg_dir.mkdir(parents=True, exist_ok=True)
    default_cfg_dir.mkdir(parents=True, exist_ok=True)
    iss_toml = (
        "# Isolated probe ISS 5-tier config\n"
        "[Misc]\n"
        "rarityConfig = [0.24, 0.24, 0.22, 0.17, 0.13]\n"
    )
    (default_cfg_dir / "irons_spellbooks-server.toml").write_text(iss_toml, encoding="utf-8")
    world_cfg_dir = SERVER / f"{mode}-{VERSION.replace('.', '-')}-world" / "serverconfig"
    world_cfg_dir.mkdir(parents=True, exist_ok=True)
    (world_cfg_dir / "irons_spellbooks-server.toml").write_text(iss_toml, encoding="utf-8")
    rarity_json = json.dumps({
        "_comment": "Seven-tier spell loot rarity distribution for Iron's Spellbooks + Forbidden Tiers",
        "rarities": {
            "common": 0.22,
            "uncommon": 0.22,
            "rare": 0.20,
            "epic": 0.16,
            "legendary": 0.11,
            "mythic": 0.06,
            "ancient": 0.03
        }
    }, indent=2) + "\n"
    (cfg_dir / "hollowrealm-tweaks-spell-rarities.json").write_text(rarity_json, encoding="utf-8")

def run(mode):
    mods = SERVER / "mods"
    mods.mkdir(exist_ok=True)
    for old in mods.glob("*.jar"):
        if old.name not in set(TARGETS) | {ARTIFACT, PROBE, "HollowRealm-Tweaks-0.1.0.jar", "hollowrealm-tweaks-probe-0.1.0.jar", "HollowRealm-Tweaks-0.2.0.jar", "hollowrealm-tweaks-probe-0.2.0.jar", "HollowRealm-Tweaks-0.2.1.jar", "hollowrealm-tweaks-probe-0.2.1.jar", "HollowRealm-Tweaks-0.3.0.jar", "hollowrealm-tweaks-probe-0.3.0.jar", "legendary_delicacies-1.1.0.jar", "legendary_delicacies-1.1.1.jar"}:
            raise RuntimeError(f"Unexpected file in dedicated test mods: {old}")
        old.unlink()
    if mode != "absent":
        for name, expected in TARGETS.items():
            source = INSTALLED_MODS / name
            if sha(source) != expected:
                raise RuntimeError(f"Source drift: {name}")
            shutil.copy2(source, mods / name)
    shutil.copy2(ROOT / "build/libs" / PROBE, mods)
    if mode != "baseline":
        shutil.copy2(ROOT / "build/libs" / ARTIFACT, mods)
    write_probe_configs(mode)
    (SERVER / "eula.txt").write_text("eula=true\n", encoding="ascii")
    settings = (
        "server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\nmax-players=1\n"
        f"level-name={mode}-{VERSION.replace('.', '-')}-world\nlevel-seed=845119\nlevel-type=minecraft:flat\n"
        'generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}\n'
        "generate-structures=false\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\n"
        "sync-chunk-writes=false\nallow-flight=true\n"
    )
    (SERVER / "server.properties").write_text(settings, encoding="ascii")
    result_file = SERVER / "verification-result.txt"
    if result_file.exists():
        result_file.unlink()
    details_file = SERVER / "verification-details.json"
    if details_file.exists():
        details_file.unlink()
    log_path = EVIDENCE / f"runtime-{mode}.log"
    argv = [str(JAVA), "-Xmx2G", f"-Dhrt.probe.mode={mode}", "@libraries/net/minecraftforge/forge/1.20.1-47.4.29/win_args.txt", "nogui"]
    start = time.monotonic()
    with log_path.open("wb") as output:
        process = subprocess.Popen(argv, cwd=SERVER, stdin=subprocess.PIPE, stdout=output, stderr=subprocess.STDOUT)
        try:
            code = process.wait(timeout=150)
        except subprocess.TimeoutExpired:
            process.stdin.write(b"stop\n")
            process.stdin.flush()
            try:
                process.wait(timeout=20)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=10)
            raise RuntimeError(f"{mode} runtime timeout; see {log_path}")
    text = log_path.read_text(encoding="utf-8", errors="replace")
    compat_errors = [
        line for line in text.splitlines()
        if any(marker in line for marker in (
            "Failed parsing gems file fallen_gems_affixes:compat/spell_lightning",
            "Failed parsing gems file fallen_gems_affixes:compat/hemolith_shard",
            "Failed parsing extra_gem_bonuses file fallen_gems_affixes:compat/hemolith_shard_leggings",
            "Unknown registry key in ResourceKey[minecraft:root / minecraft:mob_effect]: traveloptics:assassin",
        ))
    ]
    passed = code == 0 and result_file.exists() and result_file.read_text().startswith("PASS\n") and "[HRT-PROBE] PASS" in text
    if mode == "active" and compat_errors:
        passed = False
    result = {"mode": mode, "exit_code": code, "passed": passed, "seconds": round(time.monotonic() - start, 1),
              "compat_error_count": len(compat_errors),
              "compat_errors": compat_errors,
              "jars": {p.name: sha(p) for p in mods.glob("*.jar")},
              "evidence": [line for line in text.splitlines() if "[HRT-PROBE]" in line or "[HollowRealm Tweaks]" in line],
              "details": json.loads(details_file.read_text()) if details_file.exists() else {}}
    (EVIDENCE / f"runtime-{mode}.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({k:v for k,v in result.items() if k != 'details'}, ensure_ascii=False), flush=True)
    if not passed:
        print("\n".join(text.splitlines()[-35:]), flush=True)
        raise SystemExit(1)
    return result

if __name__ == "__main__":
    results = []
    for mode in ("baseline", "active", "absent"):
        results.append(run(mode))
    baseline, active, absent = results
    for phase in ("initial", "reload"):
        if baseline['details'][phase]['non_gun_affixes'] != active['details'][phase]['non_gun_affixes']:
            raise RuntimeError("Other affix registry keys changed between baseline and active")
    if baseline["compat_error_count"] == 0:
        raise RuntimeError("Expected baseline to exhibit the 3 FGA compat errors")
    if active["compat_error_count"] != 0:
        raise RuntimeError(f"Expected active to eliminate FGA compat errors, got {active['compat_error_count']}")
    (EVIDENCE / "runtime-summary.json").write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
    shutil.rmtree(SERVER / "mods", ignore_errors=True)
    print('UNCHANGED_OTHER_AFFIX_KEYS=PASS',flush=True)
    print(f'FGA_COMPAT_ERRORS_BASELINE={baseline["compat_error_count"]} ACTIVE={active["compat_error_count"]}', flush=True)
