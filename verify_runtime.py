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
VERSION = "0.2.1"
EVIDENCE = ROOT / "evidence" / VERSION
TARGETS = json.loads((EVIDENCE / "runtime-targets.json").read_text("utf-8"))
ARTIFACT = f"HollowRealm-Tweaks-{VERSION}.jar"
PROBE = f"hollowrealm-tweaks-probe-{VERSION}.jar"

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def run(mode):
    mods = SERVER / "mods"
    mods.mkdir(exist_ok=True)
    for old in mods.glob("*.jar"):
        if old.name not in set(TARGETS) | {ARTIFACT, PROBE, "HollowRealm-Tweaks-0.1.0.jar", "hollowrealm-tweaks-probe-0.1.0.jar", "HollowRealm-Tweaks-0.2.0.jar", "hollowrealm-tweaks-probe-0.2.0.jar", "legendary_delicacies-1.1.0.jar"}:
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
    passed = code == 0 and result_file.exists() and result_file.read_text().startswith("PASS\n") and "[HRT-PROBE] PASS" in text
    result = {"mode": mode, "exit_code": code, "passed": passed, "seconds": round(time.monotonic() - start, 1),
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
    (EVIDENCE / "runtime-summary.json").write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
    print('UNCHANGED_OTHER_AFFIX_KEYS=PASS',flush=True)
