const form = document.getElementById("visitForm");
const msg = document.getElementById("eligibilityMsg");
const toast = document.getElementById("toast");
const hospitalList = document.getElementById("hospitalList");
const locationStep = document.getElementById("locationStep");
const locationStatus = document.getElementById("locationStatus");
const locationDisplay = document.getElementById("locationDisplay");
const requestFormSection = document.getElementById("requestFormSection");
const selectedHospitalNameInput = document.getElementById("selectedHospitalName");
const requestUnitsInput = document.getElementById("requestUnits");
const formStep = document.getElementById("formStep");
const visitStep = document.getElementById("visitStep");
const requestSummary = document.getElementById("requestSummary");
const bloodGroupInput = document.getElementById("bloodGroup");
const emergencyLevelInput = document.getElementById("emergencyLevel");
const compatibilityText = document.getElementById("compatibilityText");
const compatibilityPills = document.getElementById("compatibilityPills");
const urgentNeededBtn = document.getElementById("urgentNeededBtn");
const emergencyBanner = document.getElementById("emergencyBanner");
let receiverConfig = { settings: {}, controls: {} };
let emergencyMode = false;
let selectedHospital = "";

const compatibility = {
  "A+": ["A+", "A-", "O+", "O-"],
  "A-": ["A-", "O-"],
  "B+": ["B+", "B-", "O+", "O-"],
  "B-": ["B-", "O-"],
  "AB+": ["AB+", "AB-", "A+", "A-", "B+", "B-", "O+", "O-"],
  "AB-": ["AB-", "A-", "B-", "O-"],
  "O+": ["O+", "O-"],
  "O-": ["O-"]
};

function showToast(text, type = "success") {
  if (!toast) return;
  toast.innerText = text;
  toast.className = `toast show ${type}`;
  setTimeout(() => toast.classList.remove("show"), 3000);
}

async function readResponsePayload(response) {
  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    try {
      return await response.json();
    } catch (error) {
      return {};
    }
  }

  const text = await response.text();
  return text ? { message: text } : {};
}

function showEligibility(status = "eligible", message = "") {
  if (!msg) return;
  msg.innerText = message || "Eligibility will be verified by the system.";
  msg.className = "eligibility " + (status === "eligible" ? "eligible" : "not-eligible");
}

function updateCompatibilityPanel() {
  if (!compatibilityText || !compatibilityPills) return;
  const selectedGroup = (bloodGroupInput && bloodGroupInput.value) || "";
  const compatibleGroups = compatibility[selectedGroup] || [];

  if (!selectedGroup) {
    compatibilityText.innerText = "Select blood group to see what can be used for this receiver.";
    compatibilityPills.innerHTML = "";
    return;
  }

  compatibilityText.innerText = `${selectedGroup} receiver can receive from:`;
  compatibilityPills.innerHTML = compatibleGroups.map((group) => `<span class="compat-pill">${group}</span>`).join("");
}

function syncEmergencyMode(forceCritical = false) {
  const selectedUrgency = (emergencyLevelInput && emergencyLevelInput.value) || "LOW";
  emergencyMode = forceCritical || selectedUrgency === "CRITICAL";

  if (emergencyLevelInput && forceCritical) {
    emergencyLevelInput.value = "CRITICAL";
  }
  if (urgentNeededBtn) {
    urgentNeededBtn.classList.toggle("active", emergencyMode);
    urgentNeededBtn.innerText = emergencyMode ? "Emergency Mode Enabled" : "Urgent Needed";
  }
  if (emergencyBanner) {
    emergencyBanner.classList.toggle("hidden", !emergencyMode);
  }
}

async function loadPublicConfig() {
  try {
    const res = await fetch("/api/admin/public-config");
    if (!res.ok) return;
    receiverConfig = await res.json();
  } catch (e) {
    receiverConfig = { settings: {}, controls: {} };
  }
}

function ensureHospitalInput() {
  if (!form) return null;
  let hospitalInput = document.getElementById("hospital");
  if (!hospitalInput) {
    hospitalInput = document.createElement("input");
    hospitalInput.type = "hidden";
    hospitalInput.id = "hospital";
    hospitalInput.name = "hospital";
    form.appendChild(hospitalInput);
  }
  return hospitalInput;
}

function openReceiverForm(hospitalName, hospitalCard) {
  selectedHospital = hospitalName || "";
  const hospitalInput = ensureHospitalInput();
  if (hospitalInput) hospitalInput.value = selectedHospital;
  if (selectedHospitalNameInput) selectedHospitalNameInput.value = selectedHospital;

  document.querySelectorAll(".hospital-card").forEach((card) => {
    card.style.borderColor = "#e5e7eb";
  });
  if (hospitalCard) hospitalCard.style.borderColor = "#dc2626";

  if (requestFormSection) requestFormSection.classList.remove("hidden");
  if (requestFormSection) requestFormSection.scrollIntoView({ behavior: "smooth", block: "center" });

  showToast(`Selected: ${selectedHospital}`);
}

function renderHospitals(data) {
  if (!hospitalList) return;

  if (!Array.isArray(data) || data.length === 0) {
    hospitalList.innerHTML = "<p>No nearby hospital found in 5 km range.</p>";
    showToast("No nearby hospitals found", "error");
    return;
  }

  hospitalList.innerHTML = "";
  const hospitalInput = ensureHospitalInput();

  data.forEach((h, index) => {
    const card = document.createElement("div");
    card.className = "hospital-card";
    const safeName = (h.name || "").replace(/"/g, "&quot;");
    card.innerHTML = `
      <h4>${h.name || "Hospital"}</h4>
      <p>${h.address || "-"}</p>
      <span>${h.distance ?? "-"} km away</span>
      <div style="margin-top:10px;">
        <button type="button" class="btn btn-main select-hospital-btn" data-name="${safeName}">Select</button>
      </div>
    `;
    hospitalList.appendChild(card);

    if (index === 0 && !selectedHospital) {
      openReceiverForm(h.name || "", card);
      if (hospitalInput) hospitalInput.value = h.name || "";
    }
  });

  document.querySelectorAll(".select-hospital-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      const card = btn.closest(".hospital-card");
      openReceiverForm(btn.dataset.name || "", card);
    });
  });

  showToast("Nearby hospitals loaded");
}

async function fetchNearbyHospitals(lat, lng) {
  try {
    const res = await fetch(`/api/hospitals/nearby?lat=${lat}&lng=${lng}`);
    if (!res.ok) throw new Error("Failed to fetch hospitals");
    const data = await res.json();
    renderHospitals(data);
    if (locationStep) locationStep.classList.add("hidden");
  } catch (error) {
    if (locationStatus) {
      locationStatus.innerText = "Unable to load nearby hospitals.";
    }
    showToast("Hospital service error", "error");
  }
}

function getLocation() {
  if (locationStep) locationStep.classList.remove("hidden");
  if (locationStatus) locationStatus.innerText = "Detecting your location...";

  if (!navigator.geolocation) {
    if (locationStatus) locationStatus.innerText = "Geolocation is not supported.";
    showToast("Geolocation not supported", "error");
    return;
  }

  navigator.geolocation.getCurrentPosition(
    (position) => {
      const lat = position.coords.latitude;
      const lng = position.coords.longitude;

      if (locationStatus) locationStatus.innerText = "Location detected.";
      if (locationDisplay) locationDisplay.innerText = `Latitude: ${lat.toFixed(5)}, Longitude: ${lng.toFixed(5)}`;

      fetchNearbyHospitals(lat, lng);
    },
    () => {
      if (locationStatus) locationStatus.innerText = "Location permission denied.";
      showToast("Please allow location permission", "error");
    },
    { enableHighAccuracy: true, timeout: 10000 }
  );
}

function validatePayload(payload) {
  if (!payload.hospital) return "Please select hospital";
  if (!payload.patientName || payload.patientName.trim().length < 2) return "Please enter patient name";
  if (!payload.bloodGroup) return "Please select blood group";
  if (!payload.city || payload.city.trim().length < 2) return "Please enter city";
  if (!/^[0-9]{10}$/.test(payload.contactNumber)) return "Please enter a valid 10 digit contact number";
  if (!Number.isFinite(payload.unitsRequired) || payload.unitsRequired <= 0) return "Please enter valid units";
  return "";
}

async function submitMedicalProof(requestId) {
  const fileInput = document.getElementById("medicalProof");
  if (!fileInput || !fileInput.files || fileInput.files.length === 0 || !requestId) {
    return { success: true };
  }

  const fd = new FormData();
  fd.append("file", fileInput.files[0]);

  try {
    const uploadRes = await fetch(`/api/blood-request/upload-proof/${requestId}`, {
      method: "POST",
      body: fd,
      credentials: "include"
    });

    if (!uploadRes.ok) {
      const uploadPayload = await readResponsePayload(uploadRes);
      return { success: false, message: uploadPayload.message || "Medical proof upload failed." };
    }
  } catch (error) {
    return { success: false, message: "Medical proof upload failed." };
  }

  return { success: true };
}

window.getLocation = getLocation;

showEligibility();
ensureHospitalInput();
loadPublicConfig().then(() => {
  const controls = receiverConfig.controls || {};
  if (controls.maintenanceMode === true || controls.showBloodRequest === false || controls.enableRequests === false) {
    showEligibility("not-eligible", "Blood request feature is currently disabled by admin.");
    if (form) form.style.display = "none";
    if (locationStep) locationStep.classList.add("hidden");
    return;
  }
  getLocation();
});

if (form) {
  form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const hospital = (document.getElementById("hospital") || {}).value || selectedHospital;
    const units = Number((requestUnitsInput && requestUnitsInput.value) || 0);
    const emergencyLevel = (document.getElementById("emergencyLevel") || {}).value || "LOW";

    const controls = receiverConfig.controls || {};
    if (controls.maintenanceMode === true || controls.showBloodRequest === false || controls.enableRequests === false) {
      showToast("Blood request feature is currently disabled by admin", "error");
      return;
    }

    const payload = {
      patientName: ((document.getElementById("patientName") || {}).value || "").trim(),
      hospital,
      unitsRequired: units,
      bloodGroup: (document.getElementById("bloodGroup") || {}).value || "",
      city: ((document.getElementById("city") || {}).value || "").trim(),
      contactNumber: ((document.getElementById("contactNumber") || {}).value || "").trim(),
      urgency: emergencyMode ? "CRITICAL" : emergencyLevel,
      description: ""
    };

    const validationMessage = validatePayload(payload);
    if (validationMessage) {
      showToast(validationMessage, "error");
      return;
    }

    const endpoint = payload.urgency === "CRITICAL" ? "/api/blood-request/create-emergency" : "/api/blood-request/create";
    const submitButton = form.querySelector('button[type="submit"]');

    try {
      if (submitButton) {
        submitButton.disabled = true;
        submitButton.textContent = "Submitting...";
      }

      const res = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(payload)
      });

      const responsePayload = await readResponsePayload(res);

      if (res.status === 401) {
        showToast(responsePayload.message || "Please login first", "error");
        setTimeout(() => {
          window.location.href = "/login";
        }, 1200);
        return;
      }

      if (!res.ok) {
        showToast(responsePayload.message || "Unable to submit request", "error");
        return;
      }

      if (requestSummary) {
        requestSummary.innerHTML = `
          <p><strong>Hospital:</strong> ${responsePayload.request.hospital}</p>
          <p><strong>Units:</strong> ${responsePayload.request.unitsRequired}</p>
          <p><strong>Request ID:</strong> ${responsePayload.request.publicId || ""}</p>
          <p><strong>Status:</strong> ${responsePayload.request.status}</p>
          <p><strong>Compatible:</strong> ${(responsePayload.compatibleBloodGroups || []).join(", ") || "-"}</p>
        `;
      }

      if (formStep) formStep.classList.add("hidden");
      if (visitStep) visitStep.classList.remove("hidden");
      showToast(responsePayload.message || "Request submitted successfully");

      const reqId = responsePayload.request.id;
      const uploadResult = await submitMedicalProof(reqId);
      if (!uploadResult.success) {
        showToast(uploadResult.message, "warn");
      }

      setTimeout(() => {
        window.location.href = `/receiver-track?requestId=${reqId}`;
      }, 1000);
    } catch (error) {
      showToast("Unable to submit request", "error");
    } finally {
      if (submitButton) {
        submitButton.disabled = false;
        submitButton.textContent = "Submit Request";
      }
    }
  });
}

if (bloodGroupInput) {
  bloodGroupInput.addEventListener("change", updateCompatibilityPanel);
  updateCompatibilityPanel();
}

if (emergencyLevelInput) {
  emergencyLevelInput.addEventListener("change", () => syncEmergencyMode(false));
  syncEmergencyMode(emergencyLevelInput.value === "CRITICAL");
}

if (urgentNeededBtn) {
  urgentNeededBtn.addEventListener("click", () => syncEmergencyMode(true));
}
