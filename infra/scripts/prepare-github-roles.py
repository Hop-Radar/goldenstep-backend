"""Read GitHub repository IDs and prepare immutable OIDC role parameters."""
import argparse
import json
from pathlib import Path
import re
import shutil
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    gh = shutil.which("gh")
    if not gh:
        parser.error("Install GitHub CLI, reopen Git Bash, then run gh auth login")
    parameters = []
    owner_id = None
    for component, repository in (
        ("Backend", "goldenstep-backend"),
        ("Frontend", "goldenstep-frontend"),
        ("Algorithm", "goldenstep-algorithm"),
    ):
        result = subprocess.run(
            [gh, "api", f"repos/Hop-Radar/{repository}",
             "--jq", "{id: .id, owner_id: .owner.id, full_name: .full_name}"],
            capture_output=True, text=True,
        )
        if result.returncode:
            parser.error(f"Cannot read Hop-Radar/{repository}; check gh auth login and repository access")
        metadata = json.loads(result.stdout)
        if metadata.get("full_name") != f"Hop-Radar/{repository}":
            parser.error("Repository name changed; verify the deployment template")
        for key in ("id", "owner_id"):
            if not re.fullmatch(r"[1-9][0-9]*", str(metadata.get(key, ""))):
                parser.error("GitHub returned an invalid repository or owner ID")
        if owner_id is not None and owner_id != metadata["owner_id"]:
            parser.error("Repositories must belong to the same GitHub owner")
        owner_id = metadata["owner_id"]
        subject = (f"repo:Hop-Radar@{owner_id}/{repository}@{metadata['id']}"
                   ":ref:refs/heads/main")
        parameters.append({"ParameterKey": component + "OidcSubject",
                           "ParameterValue": subject})
        # Only repository identity metadata is printed, never a GitHub token.
        print(component + ": " + subject)
    Path(args.output).write_text(json.dumps(parameters, indent=2) + "\n", encoding="utf-8")
    print("GitHub OIDC role parameters: READY")


if __name__ == "__main__":
    main()
