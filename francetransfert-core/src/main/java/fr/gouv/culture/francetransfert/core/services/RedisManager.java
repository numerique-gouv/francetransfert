/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.services;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.exception.ErrorEnum;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import jakarta.annotation.PostConstruct;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.Pool;

@Service
public class RedisManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(RedisManager.class);

	private static final String JEDIS_NULL = "Jedis is null";

	@Value("${metaload.host}")
	private String host;

	@Value("${metaload.port}")
	private int port;

	@Value("${metaload.password:#{null}}")
	private String password;

	@Value("${metaload.sentinel.active}")
	private String sentinelActive;

	@Value("${metaload.sentinel.nodes}")
	private String sentinelNodes;

	@Value("${metaload.sentinel.master.name}")
	private String sentinelMasterName;

	@Value("${metaload.poolconfig.maxTotal}")
	private int maxTotal;

	@Value("${metaload.poolconfig.maxIdle}")
	private int maxIdle;

	@Value("${metaload.poolconfig.minIdle}")
	private int minIdle;

	@Value("${metaload.poolconfig.maxWaitMillis}")
	private int maxWaitMillis;

	@Value("${metaload.poolconfig.minEvictableIdleTimeMillis}")
	private int minEvictableIdleTimeMillis;

	@Value("${metaload.poolconfig.timeBetweenEvictionRunsMillis}")
	private int timeBetweenEvictionRunsMillis;

	@Value("${metaload.poolconfig.numTestsPerEvictionRun}")
	private int numTestsPerEvictionRun;

	@Value("${metaload.poolconfig.testOnBorrow}")
	private boolean testOnBorrow;

	@Value("${metaload.poolconfig.testOnReturn}")
	private boolean testOnReturn;

	@Value("${metaload.poolconfig.testWhileIdle}")
	private boolean testWhileIdle;

	@Value("${metaload.poolconfig.blockWhenExhausted}")
	private boolean blockWhenExhausted;

	@Value("${expire.token.sender:1800}")
	private int expireTokenSender;

	private Pool<Jedis> pool = null;

	private JedisPoolConfig poolConfig;

	public void setPool(Pool pool) {
		this.pool = pool;
	}

	@PostConstruct
	private void setUpProps() {
		if (this.pool == null) {
			this.poolConfig = buildPoolConfig();
			if (Boolean.valueOf(sentinelActive)) {
				setSentinelNodes(sentinelNodes);
				setSentinelMasterName(sentinelMasterName);
				String[] sentinelsArray = getSentinelNodes().split(",");
				Set<String> sentinels = new HashSet<>();
				for (String sentinel : sentinelsArray) {
					sentinels.add(sentinel);
				}
				if (StringUtils.isBlank(password)) {
					this.pool = new JedisSentinelPool(getSentinelMasterName(), sentinels, poolConfig);
				} else {
					this.pool = new JedisSentinelPool(getSentinelMasterName(), sentinels, poolConfig,
							Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, password, Protocol.DEFAULT_DATABASE,
							null, Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, password, null);
					// this.pool = new JedisSentinelPool(getSentinelMasterName(), sentinels,
					// password, password);
				}
			} else {
				setHost(host);
				setPort(port);
				if (StringUtils.isBlank(password)) {
					this.pool = new JedisPool(poolConfig, getHost(), getPort());
				} else {
					this.pool = new JedisPool(poolConfig, getHost(), getPort(), Protocol.DEFAULT_TIMEOUT, password);
				}

			}
		}
	}

	private JedisPoolConfig buildPoolConfig() {
		final JedisPoolConfig poolConfigTmp = new JedisPoolConfig();
		poolConfigTmp.setMaxTotal(maxTotal);
		poolConfigTmp.setMaxIdle(maxIdle);
		poolConfigTmp.setMinIdle(minIdle);
		poolConfigTmp.setMaxWaitMillis(maxWaitMillis);
		poolConfigTmp.setTestOnBorrow(testOnBorrow);
		poolConfigTmp.setTestOnReturn(testOnReturn);
		poolConfigTmp.setTestWhileIdle(testWhileIdle);
		poolConfigTmp.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
		poolConfigTmp.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
		poolConfigTmp.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
		poolConfigTmp.setBlockWhenExhausted(blockWhenExhausted);
		return poolConfigTmp;
	}

	public void returnResource(Jedis jedis) {
		if (jedis != null) {
			try {
				jedis.close();
			} catch (Exception e) {
				throw e;
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void returnBrokenResource(Jedis jedis, String name, Exception msge) {
		LOGGER.error("Error brokenRessource : " + msge.getMessage(), msge);
		if (jedis != null) {
			try {
				jedis.close();
			} catch (Exception e) {
				throw e;
			}
		}
	}

	public void expireToken(String senderMail, String token) throws MetaloadException {

		checkTokenValidity(senderMail, token);
		String tokenKey = RedisKeysEnum.FT_TOKEN_SENDER.getKey(senderMail) + ":" + token;
		boolean tokenExist = exists(tokenKey);
		if (tokenExist) {
			expire(tokenKey, 1);
		}
	}

	public void extendTokenValidity(String senderMail, String token) throws MetaloadException {

		checkTokenValidity(senderMail, token);
		String tokenKey = RedisKeysEnum.FT_TOKEN_SENDER.getKey(senderMail.toLowerCase()) + ":" + token;
		boolean tokenExist = exists(tokenKey);
		if (tokenExist) {
			int secondToExpire = expireTokenSender;
			expire(tokenKey, secondToExpire);
		}

	}

	public void validateToken(String senderMail, String token) throws MetaloadException {
		// verify token in redis
		LOGGER.debug("check token for sender mail {}", senderMail);
		checkTokenValidity(senderMail, token);
	}

	private void checkTokenValidity(String senderMail, String token) throws MetaloadException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof AnonymousAuthenticationToken)) {
			if (senderMail.equalsIgnoreCase(authentication.getName())) {
				return;
			} else {
				throw new MetaloadException("Invalid SSO token");
			}
		} else if (token != null && !token.equalsIgnoreCase("unknown")) {
			boolean tokenExistInRedis;
			try {
				String tokenKey = RedisKeysEnum.FT_TOKEN_SENDER.getKey(senderMail.toLowerCase()) + ":" + token;
				tokenExistInRedis = exists(tokenKey);
			} catch (Exception e) {
				String uuid = UUID.randomUUID().toString();
				throw new MetaloadException(
						ErrorEnum.TECHNICAL_ERROR.getValue() + " validating token : " + e.getMessage(), uuid, e);
			}
			if (!tokenExistInRedis) {
				throw new MetaloadException("Invalid token: token does not exist in redis");
			}
		} else {
			throw new MetaloadException("Invalid token");
		}
	}

	public boolean expire(String key, int seconds) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			if (seconds >= 0) {
				ret = (jedis.expire(key, seconds) > 0);
			}
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "expire:" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public boolean setString(String key, String object) {
		return setString(key, object, -1);
	}

	public boolean setNxString(String key, String value, int seconds) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = (jedis.setnx(key, value) > 0);
			if (seconds >= 0) {
				jedis.expire(key, seconds);
			}
		} catch (Exception e) {
			success = false;
			releaseBrokenReidsSource(jedis, key, "setNxString", e, false);
			throw e;
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public boolean setHnxString(String key, String field, String value) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = (jedis.hsetnx(key, field, value) > 0);
		} catch (Exception e) {
			success = false;
			releaseBrokenReidsSource(jedis, key, "setHnxString", e, false);
			throw e;
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public String getHgetString(String key, String field) {
		Jedis jedis = null;
		boolean success = true;
		String ret = null;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.hget(key, field);
		} catch (Exception e) {
			success = false;
			releaseBrokenReidsSource(jedis, key, "getHString", e, false);
			throw e;
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public boolean deleteHField(String key, String field) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = (jedis.hdel(key, field) > 0);
		} catch (Exception e) {
			success = false;
			releaseBrokenReidsSource(jedis, key, "deleteHField", e, false);
			throw e;
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public boolean deleteKey(String key) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = (jedis.del(key) > 0);
		} catch (Exception e) {
			success = false;
			releaseBrokenReidsSource(jedis, key, "deleteKey", e, false);
			throw e;
		} finally {
			releaseRedisSource(success, jedis);
		}

		return ret;
	}

	public void deleteKeys(String... keys) {
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				throw new JedisException(JEDIS_NULL);
			}
			jedis.del(keys);
		} catch (Exception e) {
			returnBrokenResource(jedis, "deleteKey" + keys, e);
		} finally {
			releaseRedisSource(true, jedis);
		}

	}

	private void releaseBrokenReidsSource(Jedis jedis, String key, String string, Exception e, boolean deleteKeyFlag) {
		returnBrokenResource(jedis, string, e);
		if (deleteKeyFlag) {
			expire(key, 0);
		}
	}

	private void releaseRedisSource(boolean success, Jedis jedis) {
		if (success && jedis != null) {
			returnResource(jedis);
		}
	}

	public boolean setString(String key, String value, int seconds) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = (jedis.set(key, value) != null);
			if (seconds >= 0) {
				jedis.expire(key, seconds);
			}
		} catch (Exception e) {
			success = false;
			releaseBrokenReidsSource(jedis, key, "setString", e, true);
			throw e;
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public String getString(String key) {
		return getString(key, -1);
	}

	public String getString(String key, int seconds) {
		Jedis jedis = null;
		boolean success = true;
		String ret = null;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.get(key);
			if (seconds >= 0) {
				jedis.expire(key, seconds);
			}
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "getString", e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public boolean hexists(String key, String field) {
		Jedis jedis = null;
		boolean success = true;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			return jedis.hexists(key, field);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "hexists", e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return false;
	}

	public boolean sexists(String key, String member) {
		Jedis jedis = null;
		boolean success = true;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			return jedis.sismember(key, member);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "hexists", e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return false;
	}

	public Long hdel(String key, String... fields) {
		Jedis jedis = null;
		boolean success = true;
		Long ret = -1L;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.hdel(key, fields);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "hdel", e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public void hsetString(String key, String field, String value, int seconds) {
		Jedis jedis = null;
		boolean success = true;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			jedis.hset(key, field, value);
			if (seconds >= 0) {
				jedis.expire(key, seconds);
			}
		} catch (Exception e) {
			success = false;
			releaseBrokenReidsSource(jedis, key, "hsetString", e, true);
			throw e;
		} finally {
			releaseRedisSource(success, jedis);
		}
	}

	public Map<String, String> hmgetAllString(String key) {
		Jedis jedis = null;
		boolean success = true;
		Map<String, String> ret = null;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.hgetAll(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "hmgetAllString" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public String hget(String key, String field) {
		Jedis jedis = null;
		boolean success = true;
		String ret = null;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.hget(key, field);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "hmgetString" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public long hLen(String key) {
		Jedis jedis = null;
		boolean success = true;
		long ret = -1;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.hlen(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "hLen" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public long lpushString(String key, String value) {
		Jedis jedis = null;
		boolean success = true;
		long ret = 0;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.lpush(key, value);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "lpushString" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public long rPushString(String key, String value) {
		Jedis jedis = null;
		boolean success = true;
		long ret = 0;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.rpush(key, value);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "rpushString" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public boolean exists(String key) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.exists(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "exists:" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public Set<String> keys(String key) {
		Jedis jedis = null;
		boolean success = true;
		Set<String> keys = null;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			keys = jedis.keys("*" + key + "*");
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "keys", e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return keys;
	}

	public long rpush(String key, String value) {
		Jedis jedis = null;
		boolean success = true;
		long ret = -1;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.rpush(key, value);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "rpush key:" + key + "value:" + value, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public long lpush(String key, String value) {
		Jedis jedis = null;
		boolean success = true;
		long ret = -1;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.lpush(key, value);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "lpush key:" + key + "value:" + value, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public Long publish(String channel, String message) {
		Jedis jedis = null;
		boolean success = true;
		Long ret = null;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.publish(channel, message);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "publish channel:" + channel, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public void subscribe(JedisPubSub jedisPubSub, String channels) {
		Jedis jedis = null;
		boolean success = true;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			jedis.subscribe(jedisPubSub, channels);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "publish channel:" + channels, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
	}

	public List<String> hvals(String key) {
		Jedis jedis = null;
		boolean success = true;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			return jedis.hvals(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "hvals", e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return null;
	}

	public List<String> subscribeFT(String key) {
		Jedis jedis = null;
		boolean success = true;
		List<String> ret = null;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.blpop(0, key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "Boken Lpop:" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public String brpoplpush(String source, String destination, int timeout) {
		Jedis jedis = null;
		boolean success = true;
		String ret = null;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.brpoplpush(source, destination, timeout);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "Boken brpoplpush source: " + source + " and destination: " + destination, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public Long publishFT(String key, String message) {
		Jedis jedis = null;
		boolean success = true;
		Long ret = null;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.rpush(key, message);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "publish Key:" + key + "message: " + message, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public String lpop(String key) {
		Jedis jedis = null;
		boolean success = true;
		String ret = null;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.lpop(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "lpop key:" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public long scardString(String key) {
		Jedis jedis = null;
		boolean success = true;
		long ret = 0L;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.scard(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "scardString" + key, e);
			LOGGER.debug("scardString : " + e.getMessage(), e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public long saddString(String key, String value) {
		Jedis jedis = null;
		boolean success = true;
		Long ret = 0L;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.sadd(key, value);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "saddString" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public long saddStrings(String key, String... values) {
		Jedis jedis = null;
		boolean success = true;
		long ret = 0;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.sadd(key, values);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "saddStrings" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public long sremString(String key, String value) {
		Jedis jedis = null;
		boolean success = true;
		long ret = 0L;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.srem(key, value);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "sremString" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public long sremStrings(String key, String... values) {
		Jedis jedis = null;
		boolean success = true;
		long ret = 0L;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.srem(key, values);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "sremStrings" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public String spopString(String key) {
		Jedis jedis = null;
		boolean success = true;
		String ret = null;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.spop(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "spopString" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public Set<String> smembersString(String key) {
		Jedis jedis = null;
		boolean success = true;
		Set<String> ret = null;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.smembers(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "smembersString" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public long zRemByMember(String key, String member) {
		Jedis jedis = null;
		boolean success = true;
		long ret = 0L;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.zrem(key, member);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "zrangeByScoreWithScores", e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public boolean zRemByMemberReturnBoolean(String key, String member) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = (jedis.zrem(key, member) > 0);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "zrangeByScoreWithScores", e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public Set<String> zrangeByScore(String key, long min, long max, int limit) {
		Jedis jedis = null;
		boolean success = true;
		Set<String> ret = null;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = new HashSet<>(jedis.zrangeByScore(key, min, max, 0, limit));
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "zrangeByScore", e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public boolean zAdd(String key, String member, long value) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = (jedis.zadd(key, value, member) > 0);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "zAdd key:" + key + "member:" + member + "value:" + value, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public boolean zIncrBy(String key, String member, long value) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}

			ret = (jedis.zincrby(key, value, member) > 0);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "zIncrBy key:" + key + "member:" + member + "value:" + value, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public boolean zAddMap(String key, Map<String, Double> scoreMembers) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = (jedis.zadd(key, scoreMembers) > 0);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "zAddMap key:" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public Long incr(String key) {
		Jedis jedis = null;
		boolean success = true;
		long ret = -1;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.incr(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "incr:" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public Long incrBy(String key, long value) {
		Jedis jedis = null;
		boolean success = true;
		long ret = -1;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.incrBy(key, value);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "incrBy:" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public boolean decr(String key) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = (jedis.decr(key) > 0);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "decr:" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public boolean decrBy(String key, int size) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = (jedis.decrBy(key, size) > 0);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "decrBy:" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	/**
	 * Wrapper passe-plat pour la méthode {@code Jedis.hincrby} respectant le
	 * pattern établi dans cette classe.
	 *
	 * @return Le nombre (stocké dans un champ de HASH) après incrémentation
	 *         atomique ; 1 au premier appel si la méthode a dû créer le HASH et/ou
	 *         le champ.
	 * @see Jedis#hincrby(String, String, long)
	 */
	public Long hincrBy(String key, String field, long value) {

		Jedis jedis = null;
		boolean success = true;
		long ret = -1;

		try {

			jedis = pool.getResource();

			if (jedis == null) {

				success = false;
				throw new JedisException(JEDIS_NULL);
			}

			ret = jedis.hincrBy(key, field, value);
		} catch (Exception e) {

			success = false;
			returnBrokenResource(jedis, "hincrBy:" + key + "," + field, e);
		} finally {
			releaseRedisSource(success, jedis);
		}

		return ret;
	}

	public String rpop(String key) {
		Jedis jedis = null;
		boolean success = true;
		String ret = null;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.rpop(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "lpop key:" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public String lindex(String key) {
		Jedis jedis = null;
		boolean success = true;
		String ret = null;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.lindex(key, 0);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "lpop key:" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public void insertHASH(String id, Map<String, String> properties) {
		Jedis jedis = null;
		boolean success = true;
		try {
			jedis = pool.getResource();
			jedis.hmset(id, properties);

		} catch (Exception e) {
			LOGGER.error("Error insert HASH : " + e.getMessage(), e);
			success = false;
			returnBrokenResource(jedis, "lrange", e);
		} finally {
			releaseRedisSource(success, jedis);
		}
	}

	public List<String> lrange(String key, int start, int stop) {
		Jedis jedis = null;
		boolean success = true;
		List<String> ret = null;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.lrange(key, start, stop);

		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "lrange", e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public void insertList(String key, List<String> list) {
		Jedis jedis = null;
		boolean success = true;
		try {
			jedis = pool.getResource();
			for (String value : list) {
				if (value == list.get(0)) {
					jedis.lpush(key, value);
				} else {
					jedis.rpush(key, value);
				}
			}
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "lpush key:" + key, e);
			throw e;
		} finally {
			releaseRedisSource(success, jedis);
		}
	}

	public void addToList(String key, String value) {
		Jedis jedis = null;
		boolean success = true;
		try {
			jedis = pool.getResource();
			jedis.rpush(key, value);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "lpush key:" + key, e);
			throw e;
		} finally {
			releaseRedisSource(success, jedis);
		}
	}

	/**
	 * @param source
	 * @param destination
	 */
	public void sInterStore(String source, String destination) {
		Jedis jedis = null;
		boolean success = true;
		try {
			jedis = pool.getResource();
			jedis.sinterstore(source, destination);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "lpush key:" + source, e);
			throw e;
		} finally {
			releaseRedisSource(success, jedis);
		}
	}

	/**
	 *
	 * @param key
	 * @param values
	 */
	public void saddString(String key, List<String> values) {
		Jedis jedis = null;
		boolean success = true;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			for (String value : values) {
				jedis.sadd(key, value);
			}
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "saddString" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
	}

	public String renameKey(String oldKey, String newKey) {
		Jedis jedis = null;
		boolean success = true;
		String ret = "KO";
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}

			ret = jedis.rename(oldKey, newKey);
		} catch (Exception e) {
			success = false;
			throw e;
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public long ttl(String key) {
		Jedis jedis = null;
		boolean success = true;
		long ret = -1;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.ttl(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "ttl:" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public String ping() {
		Jedis jedis = null;
		boolean success = true;
		String ret = "";
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.ping();
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "ping", e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public long lrem(String key, long count, String value) {
		Jedis jedis = null;
		boolean success = true;
		long ret = 0L;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.lrem(key, count, value);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "lrem" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	public long srem(String key, String value) {
		Jedis jedis = null;
		boolean success = true;
		long ret = 0L;
		try {
			jedis = pool.getResource();
			if (jedis == null) {
				success = false;
				throw new JedisException(JEDIS_NULL);
			}
			ret = jedis.srem(key, value);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "srem" + key, e);
		} finally {
			releaseRedisSource(success, jedis);
		}
		return ret;
	}

	protected String getHost() {
		return host;
	}

	protected void setHost(String host) {
		this.host = host;
	}

	protected int getPort() {
		return port;
	}

	protected void setPort(int port) {
		this.port = port;
	}

	protected String getSentinelNodes() {
		return sentinelNodes;
	}

	protected void setSentinelNodes(String sentinelNodes) {
		this.sentinelNodes = sentinelNodes;
	}

	protected String getSentinelMasterName() {
		return sentinelMasterName;
	}

	protected void setSentinelMasterName(String sentinelMasterName) {
		this.sentinelMasterName = sentinelMasterName;
	}
}
