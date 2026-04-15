function setActiveNavLink(nav) {
  if (!nav) return;

  const currentPath = window.location.pathname.replace(/\/+$/, "") || "/";
  Array.from(nav.querySelectorAll("a[href]")).forEach((link) => {
    const href = link.getAttribute("href");
    if (!href || href.startsWith("javascript:") || href.startsWith("#")) return;

    const normalizedHref = href.replace(/\/+$/, "") || "/";
    link.classList.toggle("active", normalizedHref === currentPath);
  });
}

function closeResponsiveNav(navbar, toggle) {
  if (!navbar || !toggle) return;
  navbar.classList.remove("is-open");
  document.body.classList.remove("nav-open");
  toggle.setAttribute("aria-expanded", "false");
}

function openResponsiveNav(navbar, toggle) {
  if (!navbar || !toggle) return;
  navbar.classList.add("is-open");
  document.body.classList.add("nav-open");
  toggle.setAttribute("aria-expanded", "true");
}

function enhanceNavbar(navbar, index) {
  if (!navbar) return;

  const nav = navbar.querySelector("nav");
  if (!nav) return;

  setActiveNavLink(nav);

  if (navbar.dataset.enhanced === "true") return;
  navbar.dataset.enhanced = "true";

  const brand = document.createElement("div");
  brand.className = "navbar__brand";

  const logo = navbar.querySelector(".logo");
  if (logo) {
    brand.appendChild(logo);
  }

  const menuId = nav.id || `site-nav-${index + 1}`;
  nav.id = menuId;

  const menuWrap = document.createElement("div");
  menuWrap.className = "nav-menu";
  nav.parentNode.insertBefore(menuWrap, nav);
  menuWrap.appendChild(nav);

  const toggle = document.createElement("button");
  toggle.type = "button";
  toggle.className = "nav-toggle";
  toggle.setAttribute("aria-label", "Toggle navigation");
  toggle.setAttribute("aria-expanded", "false");
  toggle.setAttribute("aria-controls", menuId);
  toggle.innerHTML = `
    <span class="nav-toggle__icon" aria-hidden="true">
      <span class="nav-toggle__line"></span>
      <span class="nav-toggle__line"></span>
      <span class="nav-toggle__line"></span>
    </span>
  `;

  brand.appendChild(toggle);
  navbar.insertBefore(brand, menuWrap);
  navbar.classList.add("has-collapsible-nav");

  toggle.addEventListener("click", () => {
    const isOpen = navbar.classList.contains("is-open");
    if (isOpen) {
      closeResponsiveNav(navbar, toggle);
    } else {
      openResponsiveNav(navbar, toggle);
    }
  });

  Array.from(nav.querySelectorAll("a, button")).forEach((item) => {
    item.addEventListener("click", () => {
      if (window.innerWidth <= 768) {
        closeResponsiveNav(navbar, toggle);
      }
    });
  });

  document.addEventListener("click", (event) => {
    if (window.innerWidth > 768) return;
    if (!navbar.classList.contains("is-open")) return;
    if (navbar.contains(event.target)) return;
    closeResponsiveNav(navbar, toggle);
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      closeResponsiveNav(navbar, toggle);
    }
  });

  window.addEventListener("resize", () => {
    if (window.innerWidth > 768) {
      closeResponsiveNav(navbar, toggle);
    }
  });
}

document.addEventListener("DOMContentLoaded", () => {
  const navbars = Array.from(document.querySelectorAll(".navbar"));
  navbars.forEach(enhanceNavbar);

  document.querySelectorAll(".activity-table").forEach((table) => {
    if (table.parentElement && table.parentElement.classList.contains("table-responsive")) return;
    const wrapper = document.createElement("div");
    wrapper.className = "table-responsive";
    table.parentNode.insertBefore(wrapper, table);
    wrapper.appendChild(table);
  });
});
