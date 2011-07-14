
function schedCell(cell) {
	var tick = toggleTick(cell);
	unDim(cell);
	cell.className = (tick)?"selection":"grey";
	dim(cell);
}

function addSchedRow() {
	var template = document.getElementById("newSchedRow");
	var schedTable = template.parentNode;
	template = template.cloneNode(true);
	list = schedTable.getElementsByTagName("tr");
	rowIndex = list.length -1;
	list = template.getElementsByTagName("td");
	list[0].innerHTML = rowIndex;
	for(var i=2; i<list.length; i++) {
		var tick = list[i].getElementsByTagName("input")[0];
		tick.value = (i-1)*100 + rowIndex;
	}
	template.id = null;
	schedTable.appendChild(template);
	template.style.display = "";
}