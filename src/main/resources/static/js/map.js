function room_hover(self){
	// Put the element in foreground
	var floor = document.getElementById("esclangon_floor_3");
	var room = document.getElementById(self.id);
	
	var last_room = floor.firstElementChild;

	floor.removeChild(room);

	while(last_room.nodeName != "text"){
		last_room = last_room.nextElementSibling;
	}

	floor.insertBefore(room, last_room);

	var total_x = 0;
	var total_y = 0;

	for(var i = 0; i < room.points.length; i++){
		total_x = total_x + room.points[i].x
		total_y = total_y + room.points[i].y
	}

	console.log(self.id, 'x="', Math.round(total_x * 10 / room.points.length) / 10, '" y="', Math.round(total_y * 10 / room.points.length) / 10, '"');
}

function room_click(self){
	document.getElementById("pop_up").style.display = "inline";

	var room_nb = self.id.split("_");

	document.getElementById("selected_room").innerHTML = room_nb[room_nb.length - 1];
}

function pop_up_close(){
	document.getElementById("pop_up").style.display = "none";
}