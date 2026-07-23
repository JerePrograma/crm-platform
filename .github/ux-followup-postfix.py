from pathlib import Path

path = Path("backend/src/test/java/com/gestudio/crm/reporting/OperationsReportingIntegrationTest.java")
source = path.read_text(encoding="utf-8")
old = '''                "Argentina",
                null,
                null,
                null,
                null,
                null,
                null,
                externalId,
'''
new = '''                "Argentina",
                null,
                "Report contact",
                "Administration",
                externalId + "@example.test",
                null,
                null,
                externalId,
'''
count = source.count(old)
if count != 1:
    raise RuntimeError(f"Expected one reporting prospect fixture, found {count}")
path.write_text(source.replace(old, new, 1), encoding="utf-8")
print("Reporting fixture now contains a usable synthetic contact channel.")
