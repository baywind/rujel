var obj;
function toggleObj(id) {
	if(obj) {
		obj.style.display = 'none';
		if(obj.id == id) {
			obj = null;
			return obj;
		}
	}
	obj = document.getElementById(id);
	if(obj == null) return false;
		obj.style.display = 'block';
	return obj;
}
