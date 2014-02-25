package ch.cern.cms.load.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XDataDecoder {

	public static void main(String[] args) {

		String[] samples = {
				"{\"rows\":7,\"cols\":2,\"definition\":[[\"CounterName\",\"string\"],[\"DeadtimeRatio\",\"float\"]],\"data\":[[\"Deadtime\",0.010669],[\"DeadtimeBeamActive\",0.000857],[\"DeadtimeBeamActiveTriggerRules\",0.000035],[\"DeadtimeBeamActiveCalibration\",0.000827],[\"DeadtimeBeamActivePrivateOrbit\",0.000000],[\"DeadtimeBeamActivePartitionController\",0.000000],[\"DeadtimeBeamActiveTimeSlot\",0.000000]]}",
				"{\"rows\":7,\"cols\":2,\"definition\":[[\"CounterName\",\"string\"],[\"DeadtimeRatio\",\"float\"]],\"data\":[[\"Deadtime\",0.031860],[\"DeadtimeBeamActive\",0.022258],[\"DeadtimeBeamActiveTriggerRules\",0.000035],[\"DeadtimeBeamActiveCalibration\",0.000809],[\"DeadtimeBeamActivePrivateOrbit\",0.000000],[\"DeadtimeBeamActivePartitionController\",0.000008],[\"DeadtimeBeamActiveTimeSlot\",0.000000]]}",
				"{\"rows\":7,\"cols\":2,\"definition\":[[\"CounterName\",\"string\"],[\"DeadtimeRatio\",\"float\"]],\"data\":[[\"Deadtime\",0.010670],[\"DeadtimeBeamActive\",0.000858],[\"DeadtimeBeamActiveTriggerRules\",0.000036],[\"DeadtimeBeamActiveCalibration\",0.000827],[\"DeadtimeBeamActivePrivateOrbit\",0.000000],[\"DeadtimeBeamActivePartitionController\",0.000000],[\"DeadtimeBeamActiveTimeSlot\",0.000000]]}" };

		Pattern p = Pattern.compile("(\\[\"Deadtime\",)(.+?)(])");

		for (String sample : samples) {
			Matcher matcher = p.matcher(sample);
			int i = 0;
			matcher.find();
				++i;
				System.out.println(matcher.group(2));

		}

	}
}
