function getEnvironmentValue(valueName,defaultValue){
	return process.env[valueName] !== undefined ? process.env[valueName] : defaultValue
}

exports.getEnvironmentValue = getEnvironmentValue
