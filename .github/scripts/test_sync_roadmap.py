import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("sync_roadmap.py")
SPEC = importlib.util.spec_from_file_location("sync_roadmap", SCRIPT)
sync_roadmap = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = sync_roadmap
SPEC.loader.exec_module(sync_roadmap)


class RoadmapParserTest(unittest.TestCase):
    def parse(self, content: str):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "ROADMAP.md"
            path.write_text(content, encoding="utf-8")
            return sync_roadmap.parse_roadmap(path)

    def test_parses_phase_metadata_and_stable_id(self):
        tasks = self.parse(
            "## Phase One [label:enhancement] [milestone:v1.1.0]\n"
            "- [ ] Expiring links — Add a TTL <!-- roadmap-id: expiring-links -->\n"
        )
        self.assertEqual(1, len(tasks))
        self.assertEqual("expiring-links", tasks[0].task_id)
        self.assertEqual("enhancement", tasks[0].label)
        self.assertEqual("v1.1.0", tasks[0].milestone)
        self.assertEqual("Add a TTL", tasks[0].description)
        self.assertFalse(tasks[0].done)

    def test_completed_section_clears_phase_metadata(self):
        tasks = self.parse(
            "## Phase One [label:feature] [milestone:v2]\n"
            "- [ ] Planned <!-- roadmap-id: planned -->\n"
            "## Completed\n"
            "- [x] Finished <!-- roadmap-id: finished -->\n"
        )
        self.assertIsNone(tasks[1].label)
        self.assertIsNone(tasks[1].milestone)
        self.assertTrue(tasks[1].done)

    def test_rejects_missing_and_duplicate_ids(self):
        with self.assertRaisesRegex(ValueError, "requires"):
            self.parse("## Phase\n- [ ] Missing ID\n")
        with self.assertRaisesRegex(ValueError, "duplicate"):
            self.parse(
                "## Phase\n"
                "- [ ] One <!-- roadmap-id: same -->\n"
                "- [ ] Two <!-- roadmap-id: same -->\n"
            )

    def test_only_explicit_marker_claims_an_issue(self):
        self.assertEqual(
            "owned-task",
            sync_roadmap.owned_issue_id("<!-- roadmap-sync:id=owned-task -->"),
        )
        self.assertIsNone(sync_roadmap.owned_issue_id("An unrelated issue body"))


if __name__ == "__main__":
    unittest.main()
