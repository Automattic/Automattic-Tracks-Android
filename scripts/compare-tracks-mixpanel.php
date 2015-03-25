<?php
/*
 * PHP library for Mixpanel data API -- http://www.mixpanel.com/
 * Requires PHP 5.2 with JSON
 */

class Mixpanel
{
	private $api_url = 'https://data.mixpanel.com/api';
	private $version = '2.0';
	private $api_key;
	private $api_secret;

	public function __construct($api_key, $api_secret) {
		$this->api_key = $api_key;
		$this->api_secret = $api_secret;
	}

	public function request($methods, $params, $format='json') {
		// $end_point is an API end point such as events, properties, funnels, etc.
		// $method is an API method such as general, unique, average, etc.
		// $params is an associative array of parameters.
		// See http://mixpanel.com/api/docs/guides/api/

		if (!isset($params['api_key']))
			$params['api_key'] = $this->api_key;

		$params['format'] = $format;

		if (!isset($params['expire'])) {
			$current_utc_time = time() - @date('Z');
			$params['expire'] = $current_utc_time + 600; // Default 10 minutes
		}

		$param_query = '';
		foreach ($params as $param => &$value) {
			if (is_array($value))
				$value = json_encode($value);
			$param_query .= '&' . urlencode($param) . '=' . urlencode($value);
		}

		$sig = $this->signature($params);

		$uri = '/' . $this->version . '/' . join('/', $methods) . '/';
		$request_url = $uri . '?sig=' . $sig . $param_query;
		//var_dump($request_url);
		$curl_handle=curl_init();
		curl_setopt($curl_handle, CURLOPT_URL, $this->api_url . $request_url);
		curl_setopt($curl_handle, CURLOPT_CONNECTTIMEOUT, 2);
		curl_setopt($curl_handle, CURLOPT_RETURNTRANSFER, 1);
		$data = curl_exec($curl_handle);
		curl_close($curl_handle);
		return $data;
		//return json_decode($data);
	}

	private function signature($params) {
		ksort($params);
		$param_string ='';
		foreach ($params as $param => $value) {
			$param_string .= $param . '=' . $value;
		}

		return md5($param_string . $this->api_secret);
	}
}

// Example usage
 $api_key = 'XXX';
 $api_secret = 'YYY';
 $from_date =  '2015-03-24';
 $to_date =  '2015-03-25';
 $username = 'ZZZ';


//
 $mp = new Mixpanel($api_key, $api_secret);
 $data = $mp->request(array('export'), array(
     'from_date' => $from_date,
     'to_date' => $to_date,
 ));

$array = explode("\n", $data);

$json_array = array();

foreach ($array as $value) {
	$decoded_value = json_decode($value);
	if( $decoded_value->properties->distinct_id == $username) {
	 $event_name_sanitized =  str_replace(' - ', '_', strtolower($decoded_value->event));
	 $event_name_sanitized =  str_replace(' ', '_', $event_name_sanitized);
	 $decoded_value ->event = str_replace('-', '_', $event_name_sanitized);
	 $json_array[] = $decoded_value;
	}
}

$json_array = array_reverse($json_array);
//echo (json_encode($json_array, JSON_FORCE_OBJECT));

$file = 'events_from_mixpanel.txt';
unlink($file);

foreach ($json_array as $value) {
	$value_to_write = $value->properties->time.' , '.$value->event. ' , '. $value->properties->number_of_blogs."\n";
	echo $value_to_write;
	file_put_contents($file, $value_to_write, FILE_APPEND) ;
}

// Working on nosara/tracks events now.

// Do the correct query in Hive and download the result
/*
 SELECT * FROM tracks WHERE (logdateymd = '20150325' OR logdateymd = '20150324')  AND browsertype = 'php-agent'
AND userlogin ='daniloercoli' AND eventsource = 'wpandroid'
ORDER BY eventtimestamp DESC
 */

$file = fopen("query_result.csv","r"); // the file exported from Hive
$tracks_results = array();
while (($line = fgetcsv($file)) !== FALSE) {
	//$line is an array of the csv elements
	//print_r($line);
	$tracks_results[] = $line;
}
fclose($file);


$list_events_from_tracks = array();
foreach ($tracks_results as $value) {
	if( $value[6] != 'NULL' && $value[4] == $username && strpos($value[7], 'wpandroid') === 0 )
		$list_events_from_tracks[] = $value;
}

$file = 'events_from_tracks.txt';
unlink($file);
array_shift($list_events_from_tracks);
foreach ($list_events_from_tracks as $value) {
	$props = json_decode($value[8]);
	$value_to_write = substr($value[21], 0, 10).' , '.$value[6]. ' , '. $props->user_info_number_of_blogs."\n";
	echo $value_to_write;
	file_put_contents($file, $value_to_write, FILE_APPEND) ;
}
?>