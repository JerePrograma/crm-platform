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
path.write_text(source.replace(old, new, 1), encoding="utf-8")
print("Duplicate-review E2E selection made explicit.")
