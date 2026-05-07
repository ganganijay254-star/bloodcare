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

const SITE_NAV_BASE_URL = "/api/auth";
let siteNavConfigPromise = null;
let siteNavSessionPromise = null;

function siteNavCheckSession() {
  if (!siteNavSessionPromise) {
    siteNavSessionPromise = fetch(`${SITE_NAV_BASE_URL}/check-session`, { credentials: "include" }).catch(() => null);
  }
  return siteNavSessionPromise;
}

function siteNavLoadConfig() {
  if (!siteNavConfigPromise) {
    siteNavConfigPromise = fetch("/api/admin/public-config")
      .then((response) => {
        if (!response.ok) throw new Error("config");
        return response.json();
      })
      .catch(() => ({ settings: {}, controls: {} }));
  }
  return siteNavConfigPromise;
}

function siteNavControlOn(config, flag, fallback = true) {
  const controls = config && config.controls ? config.controls : {};
  if (controls[flag] === undefined || controls[flag] === null) return fallback;
  return controls[flag] !== false;
}

function siteNavLogout() {
  const doLogout = () => {
    fetch(`${SITE_NAV_BASE_URL}/logout`, { method: "POST", credentials: "include" }).finally(() => {
      window.location.href = "/";
    });
  };

  if (typeof showConfirm === "function") {
    showConfirm("Are you sure you want to logout?", doLogout);
    return;
  }

  if (window.confirm("Are you sure you want to logout?")) {
    doLogout();
  }
}

function ensureNavbarShell(navbar) {
  if (!navbar) return null;

  let logo = navbar.querySelector(".logo");
  if (!logo) {
    logo = document.createElement("div");
    logo.className = "logo";
    logo.textContent = "BloodCare";
  }

  let nav = navbar.querySelector("nav");
  if (!nav) {
    nav = document.createElement("nav");
    nav.setAttribute("data-site-nav", "");
    navbar.appendChild(nav);
  }

  if (!navbar.querySelector(".logo")) {
    navbar.insertBefore(logo, nav);
  }

  return nav;
}

function navbarNeedsLinks(nav) {
  if (!nav) return false;
  return nav.children.length === 0 || nav.hasAttribute("data-site-nav") || nav.id === "mainNav";
}

function renderSiteNavigation(nav, sessionResponse, config) {
  if (!navbarNeedsLinks(nav)) return;

  const links = [
    '<a href="/">Home</a>',
    '<a href="/about">About</a>'
  ];

  if (sessionResponse && sessionResponse.status === 200) {
    links.splice(1, 0, '<a href="/donor-dashboard">Dashboard</a>');
    if (siteNavControlOn(config, "showLeaderboard", true)) links.push('<a href="/leaderboard">Leaderboard</a>');
    if (siteNavControlOn(config, "showMedicine", true)) links.push('<a href="/medicine">Medicines</a>');
    if (siteNavControlOn(config, "showDonorProfile", true)) links.push('<a href="/donor-profile">My Profile</a>');
    links.push('<button class="btn btn-small" type="button" data-site-logout>Logout</button>');
  } else {
    links.push('<a href="/login">Login</a>');
  }

  nav.innerHTML = links.join("");
  const logoutButton = nav.querySelector("[data-site-logout]");
  if (logoutButton) logoutButton.addEventListener("click", siteNavLogout);
  setActiveNavLink(nav);
}

async function hydrateSiteNavigation(navbars) {
  const managedNavs = navbars.map(ensureNavbarShell).filter(navbarNeedsLinks);
  if (!managedNavs.length) return;

  let sessionResponse = null;
  let config = { settings: {}, controls: {} };

  try {
    const results = await Promise.all([siteNavCheckSession(), siteNavLoadConfig()]);
    sessionResponse = results[0];
    config = results[1] || config;
  } catch (e) {
    sessionResponse = null;
  }

  managedNavs.forEach((nav) => renderSiteNavigation(nav, sessionResponse, config));
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

  if (navbar.dataset.enhanced === "true") {
    navbar.classList.toggle("has-mobile-nav", nav.children.length > 1);
    return;
  }
  navbar.dataset.enhanced = "true";

  let brand = navbar.querySelector(":scope > .navbar__brand");
  if (!brand) {
    brand = document.createElement("div");
    brand.className = "navbar__brand";

    const logo = navbar.querySelector(".logo");
    if (logo) {
      brand.appendChild(logo);
    }
  }

  const menuId = nav.id || `site-nav-${index + 1}`;
  nav.id = menuId;

  let menuWrap = navbar.querySelector(":scope > .nav-menu");
  if (!menuWrap) {
    menuWrap = document.createElement("div");
    menuWrap.className = "nav-menu";
    nav.parentNode.insertBefore(menuWrap, nav);
    menuWrap.appendChild(nav);
  }

  let toggle = brand.querySelector(".nav-toggle");
  if (!toggle) {
    toggle = document.createElement("button");
    toggle.type = "button";
    toggle.className = "nav-toggle";
    toggle.setAttribute("aria-label", "Toggle navigation");
    toggle.innerHTML = `
      <span class="nav-toggle__icon" aria-hidden="true">
        <span class="nav-toggle__line"></span>
        <span class="nav-toggle__line"></span>
        <span class="nav-toggle__line"></span>
      </span>
    `;
    brand.appendChild(toggle);
  }

  toggle.setAttribute("aria-expanded", "false");
  toggle.setAttribute("aria-controls", menuId);
  if (brand.parentElement !== navbar) {
    navbar.insertBefore(brand, menuWrap);
  }
  navbar.classList.add("has-collapsible-nav");
  navbar.classList.toggle("has-mobile-nav", nav.children.length > 1);

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
  hydrateSiteNavigation(navbars).then(() => {
    navbars.forEach(enhanceNavbar);
  });
  navbars.forEach(enhanceNavbar);

  document.querySelectorAll(".activity-table").forEach((table) => {
    if (table.parentElement && table.parentElement.classList.contains("table-responsive")) return;
    const wrapper = document.createElement("div");
    wrapper.className = "table-responsive";
    table.parentNode.insertBefore(wrapper, table);
    wrapper.appendChild(table);
  });
});
