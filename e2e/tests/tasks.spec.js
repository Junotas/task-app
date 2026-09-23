import { expect, test } from "@playwright/test";

function uniqueTitle(prefix) {
    return `${prefix} ${Date.now()}-${Math.floor(Math.random() * 1000)}`;
}

test("a user can create, complete and delete a task", async ({ page }) => {
    const title = uniqueTitle("Handla mjölk");

    await page.goto("/");

    await page.getByTestId("title-input").fill(title);
    await page.getByTestId("description-input").fill("2 liter");
    await page.getByTestId("add-button").click();

    const task = page.getByTestId("task-item").filter({ hasText: title });
    await expect(task).toBeVisible();
    await expect(task.getByTestId("task-title")).toHaveText(title);

    await task.getByTestId("task-checkbox").check();
    await expect(task.getByTestId("task-checkbox")).toBeChecked();

    await page.reload();
    const taskAfterReload = page.getByTestId("task-item").filter({ hasText: title });
    await expect(taskAfterReload.getByTestId("task-checkbox")).toBeChecked();

    await taskAfterReload.getByTestId("delete-button").click();
    await expect(page.getByTestId("task-item").filter({ hasText: title })).toHaveCount(0);

    await page.reload();
    await expect(page.getByTestId("task-item").filter({ hasText: title })).toHaveCount(0);
});

test("a task is rejected when the title is empty", async ({ page }) => {
    await page.goto("/");

    const before = await page.getByTestId("task-item").count();

    await page.getByTestId("description-input").fill("Saknar titel");
    await page.getByTestId("add-button").click();

    await expect(page.getByTestId("task-item")).toHaveCount(before);
});