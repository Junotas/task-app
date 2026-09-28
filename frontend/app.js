const API_URL = "/api/tasks";

const form = document.getElementById("task-form");
const titleInput = document.getElementById("title");
const descriptionInput = document.getElementById("description");
const list = document.getElementById("task-list");
const count = document.getElementById("count");
const message = document.getElementById("message");

function showMessage(text, isError = false) {
    message.textContent = text;
    message.classList.toggle("message--error", isError);
}

function buildTaskItem(task) {
    const item = document.createElement("li");
    item.className = task.completed ? "task task--done" : "task";
    item.dataset.testid = "task-item";
    item.dataset.taskId = task.id;

    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.checked = task.completed;
    checkbox.dataset.testid = "task-checkbox";
    checkbox.setAttribute("aria-label", `Markera "${task.title}" som klar`);
    checkbox.addEventListener("change", () => toggleTask(task, checkbox.checked));

    const body = document.createElement("div");
    body.className = "task__body";

    const title = document.createElement("span");
    title.className = "task__title";
    title.dataset.testid = "task-title";
    title.textContent = task.title;
    body.appendChild(title);

    if (task.description) {
        const description = document.createElement("span");
        description.className = "task__description";
        description.textContent = task.description;
        body.appendChild(description);
    }

    const remove = document.createElement("button");
    remove.type = "button";
    remove.className = "task__remove";
    remove.dataset.testid = "delete-button";
    remove.textContent = "Ta bort";
    remove.setAttribute("aria-label", `Ta bort "${task.title}"`);
    remove.addEventListener("click", () => deleteTask(task.id));

    item.append(checkbox, body, remove);
    return item;
}

function renderTasks(tasks) {
    list.replaceChildren();

    if (tasks.length === 0) {
        const empty = document.createElement("li");
        empty.className = "task";
        empty.dataset.testid = "empty-state";
        empty.textContent = "Inget här än. Lägg till något att göra.";
        list.appendChild(empty);
    }

    for (const task of tasks) {
        list.appendChild(buildTaskItem(task));
    }

    const done = tasks.filter((task) => task.completed).length;
    count.textContent = tasks.length === 0
        ? "0 uppgifter"
        : `${done} av ${tasks.length} klara`;
}

async function loadTasks() {
    try {
        const response = await fetch(API_URL);

        if (!response.ok) {
            throw new Error(`Servern svarade ${response.status}`);
        }

        renderTasks(await response.json());
        showMessage("");
    } catch (error) {
        showMessage(`Kunde inte hämta uppgifter: ${error.message}`, true);
    }
}

async function createTask(event) {
    event.preventDefault();

    const title = titleInput.value.trim();

    if (title === "") {
        showMessage("Skriv något att göra först.", true);
        return;
    }

    try {
        const response = await fetch(API_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                title,
                description: descriptionInput.value.trim() || null,
                completed: false,
            }),
        });

        if (!response.ok) {
            throw new Error(`Servern svarade ${response.status}`);
        }

        form.reset();
        titleInput.focus();
        await loadTasks();
    } catch (error) {
        showMessage(`Kunde inte spara uppgiften: ${error.message}`, true);
    }
}

async function toggleTask(task, completed) {
    try {
        const response = await fetch(`${API_URL}/${task.id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                title: task.title,
                description: task.description,
                completed,
            }),
        });

        if (!response.ok) {
            throw new Error(`Servern svarade ${response.status}`);
        }

        await loadTasks();
    } catch (error) {
        showMessage(`Kunde inte uppdatera uppgiften: ${error.message}`, true);
        await loadTasks();
    }
}

async function deleteTask(id) {
    try {
        const response = await fetch(`${API_URL}/${id}`, { method: "DELETE" });

        if (!response.ok) {
            throw new Error(`Servern svarade ${response.status}`);
        }

        await loadTasks();
    } catch (error) {
        showMessage(`Kunde inte ta bort uppgiften: ${error.message}`, true);
    }
}

form.addEventListener("submit", createTask);
loadTasks();