import hljs from 'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/es/highlight.min.js';

document.addEventListener("DOMContentLoaded", () => {
  loadFile();
});

function loadFile() {
  const path = document.querySelector('#filePath').textContent.trim();
  const extension = getFileExtension(path);

  if (extension === 'pdf') {
    renderPDF(path);
  } else {
    renderCode(path);
  }
}

function getFileExtension(filePath) {
  return filePath.split('.').pop().toLowerCase();
}

async function renderCode(path) {
  const code = await sendRequestAsync(path, "GET");
  const textContainer = document.querySelector("#text-container");

  if (textContainer == null)
    return;

  try {
    const preElement = document.createElement("pre");
    const codeElement = document.createElement("code");
    preElement.appendChild(codeElement);
    codeElement.innerHTML = escapeHTML(code);

    hljs.highlightElement(codeElement);
    setupCopyButton(code);
    textContainer.appendChild(preElement);
  } catch (error) {
    handleError(error);
  }
}

async function renderPDF(path) {
  try {
    const pdfData = await sendRequestAsync(path, "GET", true); // Fetch as binary
    const pdfContainer = document.querySelector("#pdf-container");
    pdfContainer.innerHTML = ''; // Clear previous content

    const pdf = await pdfjsLib.getDocument({ data: pdfData }).promise;
    for (let pageNumber = 1; pageNumber <= pdf.numPages; pageNumber++) {
      const page = await pdf.getPage(pageNumber);
      const canvas = document.createElement('canvas');
      const context = canvas.getContext('2d');
      const viewport = page.getViewport({ scale: 1.5 });

      canvas.width = viewport.width;
      canvas.height = viewport.height;

      await page.render({ canvasContext: context, viewport: viewport }).promise;
      pdfContainer.appendChild(canvas);
    }
  } catch (error) {
    handleError(error);
  }
}

async function sendRequestAsync(paramPath, method = "GET", isBinary = false) {
  try {
    const response = await fetch(`/files/action/read?path=${paramPath}`, {
      method: method,
    });

    if (!response.ok) {
      throw new Error(`HTTP error! Status: ${response.status}`);
    }

    return isBinary ? new Uint8Array(await response.arrayBuffer()) : await response.text();
  } catch (error) {
    console.error('Error:', error);
    throw error;
  }
}

function setupCopyButton(data) {
  const copyButton = document.querySelector("#copyButton");
  copyButton.style.display = 'inline-block'; // Show the button
  copyButton.addEventListener("click", () => {
    navigator.clipboard.writeText(data).then(() => {
      copyButton.textContent = "Copied";
      setTimeout(() => {
        copyButton.textContent = "Copy";
      }, 3000);
    });
  });
}

function handleError(error) {
  const textContainer = document.querySelector("#text-container");
  textContainer.innerHTML = `<p style="color: red;">Error loading file: ${error.message}</p>`;
}

function escapeHTML(code) {
  return code
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

