let devices = [];
let devicesPerRoom = {};

function room_hover(self){
	// Put the element in foreground
	let floor = document.getElementById("esclangon_floor_3");
	let room = document.getElementById(self.id);

	let last_room = floor.firstElementChild;

	floor.removeChild(room);

	while(last_room.nodeName !== "text"){
		last_room = last_room.nextElementSibling;
	}

	floor.insertBefore(room, last_room);
}

function room_click(self){
	let room_nb = self.id.split("_");
	room_nb = room_nb[room_nb.length -1]

	if(!(room_nb in devicesPerRoom)){
		return;
	}

	loadRoom(room_nb);
	document.getElementById("pop_up").style.display = "inline";
	document.getElementById("selected_room").innerHTML = room_nb;
}

function pop_up_close(){
	document.getElementById("pop_up").style.display = "none";
}

function getDevices(){
	$.getJSON("/webapi/devices", function(result){
		devices = result;
		devicesPerRoom = {};
		let co2_low = 0;
		let co2_mid = 0;
		let co2_high = 0;
		let co2_sleep = 0;

		devices.forEach(device => {
			let room = device["room"];
			if(!(room in devicesPerRoom)){
				devicesPerRoom[room] = [];
			}
			devicesPerRoom[room].push(device);
			let map_room = document.getElementById("room_"+room);
			if(map_room != null) {
				if (device["lastUpdated"] === "null" || device["lastCo2Value"] === "null"
					|| (new Date()) - (new Date(device["lastUpdated"])) > 24 * 60 * 60 * 1000) {

					map_room.classList.add("room_co2_sleep");
					co2_sleep++;

				} else {
					let co2Val = device["lastCo2Value"];
					if (co2Val <= 1000) {
						map_room.classList.add("room_co2_good");
						co2_low++;
					} else if (co2Val <= 1500) {
						map_room.classList.add("room_co2_neutral");
						co2_mid++;
					} else {
						map_room.classList.add("room_co2_bad");
						co2_high++;
					}
				}
			}
		});

		document.getElementById("text-devices-low-co2").innerText = co2_low.toString();
		document.getElementById("text-devices-mid-co2").innerText = co2_mid.toString();
		document.getElementById("text-devices-high-co2").innerText = co2_high.toString();
		document.getElementById("text-devices-sleep").innerText = co2_sleep.toString();
	});
}

function loadRoom(room){
	let selectedRoomInfo = document.getElementById("selected_room_info");
	selectedRoomInfo.innerHTML = "<div class=\"spinner-border text-primary m-4\" role=\"status\"></div>";
	selectedRoomInfo.className = "w-100 d-flex justify-content-center";
	let accessDataButton = document.getElementById("selected_room_access_data_button");
	accessDataButton.href = "/devices/"+devicesPerRoom[room][0]["id"]+'/data';

	let generateForm = function(labelText, value) {
		let formGroupDiv = document.createElement("div");
		formGroupDiv.className = "form-group row";
		let label = document.createElement("label");
		label.className = "col-sm-6 col-form-label"
		label.innerText = labelText
		formGroupDiv.appendChild(label);
		let div = document.createElement("div");
		div.className = "col-sm-6";
		let input = document.createElement("input");
		input.type = "text";
		input.readOnly = true;
		input.className = "form-control-plaintext"
		input.value = value;

		div.appendChild(input);
		formGroupDiv.appendChild(div);

		return formGroupDiv;
	}

	$.getJSON("/webapi/device/"+devicesPerRoom[room][0]["id"]+"/lastValues", function (deviceLastValues) {
		console.log(deviceLastValues);
		selectedRoomInfo.innerHTML = "";
		selectedRoomInfo.className = "w-100";

		let date = new Date(deviceLastValues["timestamp"]);
		let formGroupDiv = generateForm("Dernière mise à jour", date.toLocaleString());
		selectedRoomInfo.appendChild(formGroupDiv);
		formGroupDiv = generateForm("Batterie", deviceLastValues["batterie"].toFixed(2)+" V");
		selectedRoomInfo.appendChild(formGroupDiv);
		formGroupDiv = generateForm("Température", deviceLastValues["temperature"].toFixed(2)+" °C");
		selectedRoomInfo.appendChild(formGroupDiv);
		formGroupDiv = generateForm("Humidité", deviceLastValues["humidite"].toFixed(0)+" %");
		selectedRoomInfo.appendChild(formGroupDiv);
		formGroupDiv = generateForm("CO2", deviceLastValues["co2"].toFixed(0)+" ppm");
		selectedRoomInfo.appendChild(formGroupDiv);
	})
}

getDevices();