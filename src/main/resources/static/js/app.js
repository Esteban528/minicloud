document.addEventListener("DOMContentLoaded", () => {
  dotButton();
  passwordInputButton();
})

function dotButton() {
  const dotsButton = document.querySelectorAll(".dot-button");

  dotsButton.forEach(button => {
    const dot = button.querySelector(':scope > .dot');
    if (dot.classList.contains("noeditable")) {
      return
    }
    button.addEventListener("mouseenter", () => {
      dot.classList.add("dot-hover");
    })

    button.addEventListener("mouseleave", () => {
      dot.classList.remove("dot-hover");
    })
  });
}

function passwordInputButton() {
  const passwordInputArray = document.querySelectorAll('#passwordInput');

  passwordInputArray.forEach(element => {
    const button = document.createElement("button");
    const input = element.querySelector("input");

    button.type = "button";
    button.classList.add("button-abstract");
    button.innerHTML = '<i class="bi bi-eye-fill"></i>';

    element.appendChild(button);

    button.addEventListener("click", () => {
      const inputType = input.type;
      if (inputType == "password") {
        input.type = "text";
        button.innerHTML = '<i class="bi bi-eye-slash-fill"></i>';
      } else {
        input.type = "password";
        button.innerHTML = '<i class="bi bi-eye-fill"></i>';
      }
    })
  })
}

