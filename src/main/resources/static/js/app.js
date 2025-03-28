document.addEventListener("DOMContentLoaded", () => {
  dotButton();
  passwordInputButton();
  submitButtonsAnimation();
})

function submitButtonsAnimation() {
  const formList = document.querySelectorAll("form");

  formList.forEach(form => {
    form.addEventListener("submit", (e) => {

      const submitButton = form.querySelector('button[type="submit"], input[type="submit"]');
      if (submitButton) {
        submitButton.classList.add("cyan-disabled");
        submitButton.disabled = true;
        submitButton.innerHTML = `<div class="lds-ring"><div></div><div></div><div></div><div></div></div>`;
      }
    });
  });
}

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

function togglePopup(idModal) {
  const popup = document.getElementById(idModal);
  //popup.addEventListener("click", () => popup.classList.toggle("hidden"))
  popup.classList.toggle("hidden");
}

