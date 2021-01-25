function stringify(items) {
	var lines = [];
	for (var i = 0, l = items.length; i < l; i++) {
		var item = items[i];
		lines.push(item.name);
		var subLines = stringify(item.children);
		for (var j = 0, m = subLines.length; j < m; j++) {
			lines.push("  " + subLines[j]);
		}
	}
	return lines;
}

function structurize(paths) {
	var items = [];
	for (var i = 0, l = paths.length; i < l; i++) {
		var path = paths[i];
		var name = path[0];
		var rest = path.slice(1);
		var item = null;
		for (var j = 0, m = items.length; j < m; j++) {
			if (items[j].name === name) {
				item = items[j];
				break;
			}
		}
		if (item === null) {
			item = {
				name : name,
				children : []
			};
			items.push(item);
		}
		if (rest.length > 0) {
			item.children.push(rest);
		}
	}

	for (i = 0, l = items.length; i < l; i++) {
		item = items[i];
		item.children = structurize(item.children);
	}
	return items;
}
/*
 * Variable to test 
 * var paths = [ "/person/id", 
 * 				"/person/first_name",
 * 				"/person/last_name", 
 * 				"/person/email", 
 * 				"/person/gender",
 * 				"/person/skills/skill",
 *  			"/person/ssn", 
 *  			"/person/department",
 * 				"/person/avatar", 
 * 				"/person/job", 
 * 				"/person/address/address_line1",
 * 				"/person/address/address_line2", 
 * 				"/person/address/city",
 * 				"/person/address/state", 
 * 				"/person/address/country", 
 * 				"/person/address/zip",
 * 				"/person/assets/cars/car/year", 
 * 				"/person/assets/cars/car/make",
 * 				"/person/assets/cars/car/model" ]
 */
function invoke(paths) {
	return JSON.stringify(structurize(paths.map(function(path) {
		return path.replace('/', '').split('/');
	})));
}