from pathlib import Path

path = Path("frontend/tests/complete-crm.spec.ts")
source = path.read_text(encoding="utf-8")
old = '''  const duplicateReview = page
    .locator("article.duplicate-review-card")
    .filter({ hasText: `E2E Syntetic ${suffix}` });
  await expect(duplicateReview).toBeVisible();
  await duplicateReview.getByRole("button", { name: "Vincular con el existente" }).click();
  const linkDialog = page.getByRole("dialog", { name: "Vincular con el existente" });
  await expect(linkDialog).toBeVisible();
  await linkDialog.getByRole("button", { name: "Vincular con el existente" }).click();
  await expect(duplicateReview).toHaveCount(0);'''
new = '''  const duplicateReviews = page
    .locator("article.duplicate-review-card")
    .filter({ hasText: `E2E Syntetic ${suffix}` });
  await expect(duplicateReviews).toHaveCount(2);
  const duplicateReview = duplicateReviews.first();
  await duplicateReview.getByRole("button", { name: "Vincular con el existente" }).click();
  const linkDialog = page.getByRole("dialog", { name: "Vincular con el existente" });
  await expect(linkDialog).toBeVisible();
  await linkDialog.getByRole("button", { name: "Vincular con el existente" }).click();
  await expect(duplicateReviews).toHaveCount(1);'''
count = source.count(old)
if count != 1:
    raise RuntimeError(f"Expected one transformed duplicate-review block, found {count}")
source = source.replace(old, new, 1)

old_heading_assertion = 'await expect(page.getByText("Recepción de prueba", { exact: true })).toBeVisible();'
new_heading_assertion = 'await expect(page.getByRole("heading", { name: "Recepción de prueba", exact: true })).toBeVisible();'
count = source.count(old_heading_assertion)
if count != 1:
    raise RuntimeError(f"Expected one inbound heading assertion, found {count}")
source = source.replace(old_heading_assertion, new_heading_assertion, 1)

path.write_text(source, encoding="utf-8")
print("Duplicate-review and inbound E2E selections made explicit.")
