package com.redislabs.lettusearch.output;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.redislabs.lettusearch.search.Document;
import com.redislabs.lettusearch.search.SearchResults;

import io.lettuce.core.LettuceStrings;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.MapOutput;

public class SearchOutput<K, V> extends CommandOutput<K, V, SearchResults<K, V>> {

	private Document<K, V> current;
	private MapOutput<K, V> nested;
	private int mapCount = -1;
	private final List<Integer> counts = new ArrayList<>();
	private boolean withScores;

	public SearchOutput(RedisCodec<K, V> codec, boolean withScores) {
		super(codec, new SearchResults<>());
		nested = new MapOutput<>(codec);
		this.withScores = withScores;
	}

	@Override
	public void set(ByteBuffer bytes) {
		if (current == null) {
			current = new Document<>();
			if (bytes != null) {
				current.setId(codec.decodeKey(bytes));
			}
			output.add(current);
		} else {
			if (withScores && current.getScore() == null) {
				if (bytes != null) {
					current.setScore(LettuceStrings.toDouble(decodeAscii(bytes)));
				}
			} else {
				nested.set(bytes);
			}
		}
	}

	@Override
	public void set(long integer) {
		output.setCount(integer);
	}

	@Override
	public void complete(int depth) {
		if (!counts.isEmpty()) {
			if (nested.get().size() == counts.get(0)) {
				counts.remove(0);
				current.putAll(nested.get());
				nested = new MapOutput<>(codec);
				current = null;
			}
		}
	}

	@Override
	public void multi(int count) {
		nested.multi(count);
		if (mapCount == -1) {
			mapCount = count;
		} else {
			// div 2 because of key value pair counts twice
			counts.add(count / 2);
		}
	}

}
