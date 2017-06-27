var TrafficMonitor = (function(conf) {

	if (window.$ !== undefined) {
		conf = $.extend({
			map : "map",
			latitude : 51.530784,
			longitude : -0.102517,
			zoom : 15,
			zoomMinus : 0,
			zoomPlus : 0,
			controls : false
		}, conf);
	}

	var map;
	var cars = {};
	var ambulances = {};

	var orders = {};
	var circles = {};
	var firstClicks = {};

	var currentPopupId; // Stores which vehicle or emergency (order) shows the pop-up.

	var icon = {
		car : L.MakiMarkers.icon({
			icon : "car",
			color : "#0098cc",
			size : "s"
		}),
		alarmed : L.MakiMarkers.icon({
			icon : "fire-station",
			color : "#691e7c",
			size : "m"
		}),
		ambulance : L.MakiMarkers.icon({
			icon : "hospital",
			color : "#b70132",
			size : "l"
		}),
		emergencyRed : L.MakiMarkers.icon({
			icon : "danger",
			color : "#b70132",
			size : "m"
		}),

		emergencyYellow : L.MakiMarkers.icon({
			icon : "danger",
			color : "#FFFF00",
			size : "m"
		}),

		emergencyGreen : L.MakiMarkers.icon({
			icon : "danger",
			color : "#00FF00",
			size : "m"
		}),
	};

	function init() {

		var mapConf = (conf.controls) ? {
			zoomControl : false,
			zoomAnimation : true
		} : {
			dragging : false,
			zoomControl : false,
			zoomAnimation : false,
			fadeAnimation : false
		};
		map = new L.Map(conf.map, mapConf);

		var osm = new L.TileLayer(
				'http://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png',
				{
					minZoom : conf.zoom - conf.zoomMinus,
					maxZoom : conf.zoom + conf.zoomPlus,
					attribution : '<a href="http://openstreetmap.org">OpenStreetMap</a>',
					detectRetina : true
				});

		map.setView(new L.LatLng(conf.latitude, conf.longitude), conf.zoom);
		map.addLayer(osm);

		L.control.zoom({
			position : 'bottomright'
		}).addTo(map);

		map.on('click', function(e) {
			// clickAction(e.latlng.lat, e.latlng.lng);
			updateTarget(e.latlng.lat, e.latlng.lng);
		});
		map.on('zoomend', function(e) {
			map.panTo([ conf.latitude, conf.longitude ], {
				animate : false
			})
		});
	}

	function updateTarget(lat, lng) {
		var order = L.marker([ lat, lng ], {
			icon : icon.emergencyRed
		});

		firstClicks[lat + lng] = order;
		order.addTo(map);

		clickAction(lat, lng);

	}

	function refresh() {
		map._onResize();
	}

	function setClickAction(fn) {
		clickAction = fn;
	}

	function showEmergency(emergency) {

		var ambulance = ambulances[emergency.vin];
		
		if (firstClicks[emergency.latitude + emergency.longitude] !== undefined) {
			map.removeLayer(firstClicks[emergency.latitude
					+ emergency.longitude]);
			delete firstClicks[emergency.latitude + emergency.longitude];

		}

		var status = emergency.status;
		if (status == "OPEN") {
			var order = orders[emergency.emergencyId];

			if (order === undefined) {
				order = L.marker([ emergency.latitude, emergency.longitude ], {
					icon : icon.emergencyYellow
				});
				order.addTo(map);
				orders[emergency.emergencyId] = order;
				order.on('mouseover', function(e) {
					currentPopupId = emergency.emergencyId;
					order.openPopup();
				});
				order.on('click', function(e) { // Could use 'popupclose' here, but not for car markers.
					currentPopupId = "-1";
				});
			}

			order.bindPopup("<strong>Emergency" + "<br> Status: " + emergency.status + "</strong>");

		} else if (status == "ONGING") {
			var order = orders[emergency.emergencyId];

			if (order === undefined) {
				order = L.marker([ emergency.latitude, emergency.longitude ], {
					icon : icon.emergencyGreen
				});
				order.addTo(map);
				orders[emergency.emergencyId] = order;
				order.on('mouseover', function(e) {
					currentPopupId = emergency.emergencyId;
					order.openPopup();
				});
				order.on('click', function(e) {
					currentPopupId = "-1";
				});

			} else {
				order.setIcon(icon.emergencyGreen);
			}
			
			var newAmbulanceIcon = L.MakiMarkers.icon({
				icon : "hospital",
				color : "#FFFF00",
				size : "l"
			});

			ambulance.setIcon(newAmbulanceIcon);
			ambulances[emergency.vin] = ambulance;

			order.bindPopup("<strong>Emergency" + "<br> Status: " + emergency.status + "<br> Ambulance: " + emergency.vin + "</strong>");

		} else if (status == "SOLVED") {
			if (orders[emergency.emergencyId] !== undefined) {
				orders[emergency.emergencyId].closePopup();
				map.removeLayer(orders[emergency.emergencyId]);
				delete orders[emergency.emergencyId];

			}
			ambulance.setIcon(icon.ambulance);
			ambulances[emergency.vin] = ambulance;
		}

		if (currentPopupId == emergency.emergencyId) {
			order.openPopup();
		} else {
			order.closePopup();
		}

	}

	function drawCircle(lat, lng, radius, emergencyID) {
		var circle = L.circle([ lat, lng ], radius).addTo(map);
		circles[emergencyID] = circle;
	}

	function updateAmbulance(car) {

		var c = ambulances[car.vin];
		if (c === undefined) {
			c = L.Marker.movingMarker([ [ car.latitude, car.longitude ] ], [],
					{
						icon : icon.ambulance
					});

			c.ts = new Date();
			c.addTo(map);
			ambulances[car.vin] = c;
			c.on('mouseover', function(e) {
				currentPopupId = car.vin;
				c.openPopup();
			});
			c.on('click', function(e) { // Cannot use 'popupclose', because that is triggered continuously by c.closePopup();
				currentPopupId = "-1";
			});
		}
		c.moveTo([ car.latitude, car.longitude ], (new Date() - c.ts));
		c.ts = new Date();

		if (currentPopupId == car.vin) {
			c.bindPopup("<strong>Latitude: " + car.latitude + "<br>Longitude: " + car.longitude + "<br>VIN: " + car.vin + "<br>IsFree: " + car.isFree + "</strong>");
			c.openPopup();
		} else {
			c.closePopup();
		}

	}

	function updateCar(car) {
		var c = cars[car.vin];
		if (c === undefined) {
			c = L.Marker.movingMarker([ [ car.latitude, car.longitude ] ], [],
					{
						icon : icon.car
					});

			c.ts = new Date();
			c.addTo(map);
			cars[car.vin] = c;
			c.on('mouseover', function(e) {
				currentPopupId = car.vin;
				c.openPopup();
			});
			c.on('click', function(e) { // Cannot use 'popupclose', because that is triggered continuously by c.closePopup();
				currentPopupId = "-1";
			});
		}
		c.moveTo([ car.latitude, car.longitude ], (new Date() - c.ts));
		c.ts = new Date();

		if (currentPopupId == car.vin) {
			c.bindPopup("<strong>Latitude: " + car.latitude + "<br>Longitude: " + car.longitude + "<br>VIN: " + car.vin + "</strong>");
			c.openPopup();
		} else {
			c.closePopup();
		}

	}

	function update(car) {
		if (car.vin.indexOf("ambulance") > -1) {
			updateAmbulance(car);
		} else {
			updateCar(car);
		}
	}

	return {
		init : init,
		setClickAction : setClickAction,
		update : update,
		showEmergency : showEmergency,
		refresh : refresh
	};

});
