from pathlib import Path


def replace_once(path: str, old: str, new: str, label: str) -> None:
    file = Path(path)
    source = file.read_text(encoding="utf-8")
    count = source.count(old)
    if count != 1:
        raise RuntimeError(f"Expected one {label}, found {count}")
    file.write_text(source.replace(old, new, 1), encoding="utf-8")


replace_once(
    "backend/src/test/java/com/gestudio/crm/reporting/OperationsReportingIntegrationTest.java",
    '''                "Argentina",
                null,
                null,
                null,
                null,
                null,
                null,
                externalId,
''',
    '''                "Argentina",
                null,
                "Report contact",
                "Administration",
                externalId + "@example.test",
                null,
                null,
                externalId,
''',
    "reporting prospect fixture",
)

replace_once(
    "backend/src/test/java/com/gestudio/crm/campaign/CampaignIntegrationTest.java",
    'new AudienceFilter(null, "ELIGIBLE", null, null, province, null, true, false)',
    'new AudienceFilter(null, null, null, null, province, null, true, false)',
    "campaign audience filter",
)

replace_once(
    "backend/src/test/java/com/gestudio/crm/sales/OpportunityIntegrationTest.java",
    '''                "Argentina",
                null,
                null,
                null,
                null,
                null,
                null,
                "SALES-" + suffix,
''',
    '''                "Argentina",
                null,
                "Sales contact",
                "Administration",
                "sales-" + suffix + "@example.test",
                null,
                null,
                "SALES-" + suffix,
''',
    "opportunity prospect fixture",
)

print("Reporting, campaign and opportunity fixtures aligned with contactability rules.")
