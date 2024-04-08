/*
  * Copyright (c) Ministère de la Culture (2022) 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.client.RedisTryAgainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gouv.culture.francetransfert.core.enums.EnclosureKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.FileKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RecipientKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RedisKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RootDirKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.RootFileKeysEnum;
import fr.gouv.culture.francetransfert.core.enums.SenderKeysEnum;
import fr.gouv.culture.francetransfert.core.exception.MetaloadException;
import fr.gouv.culture.francetransfert.core.services.RedisManager;

public class RedisUtils {

    private static Logger LOGGER = LoggerFactory.getLogger(RedisUtils.class);

    private RedisUtils() {
        // private Constructor
    }

    public static Map<String, String> generateMapRedis(List<String> keys, List<String> values) {
        return IntStream.range(0, Math.min(keys.size(), values.size())).boxed()
                .collect(Collectors.toMap(keys::get, values::get));
    }

    public static String generateGUID() {
        return UUID.randomUUID().toString();
    }

    public static String generateHashsha1(String value) {
        return DigestUtils.sha1Hex(value);
    }

    public static String generateHashSha256(InputStream value) throws IOException {
        return DigestUtils.sha256Hex(value);
    }

    public static String getBucketName(RedisManager redisManager, String enclosureId, String bucketNamePrefix)
            throws MetaloadException {
        LocalDateTime localDateTime = LocalDateTime.now();
        if (enclosureId != null) {
            localDateTime = DateUtils.convertStringToLocalDateTime(redisManager.getHgetString(
                    RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId), EnclosureKeysEnum.TIMESTAMP.getKey()));
        }
        return bucketNamePrefix + localDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    public static String getFileNameWithPath(RedisManager redisManager, String hashFid) throws MetaloadException {
        String key = RedisKeysEnum.FT_FILE.getKey(hashFid);
        return redisManager.getHgetString(key, FileKeysEnum.REL_OBJ_KEY.getKey());
    }

    public static String getEmailSenderEnclosure(RedisManager redisManager, String enclosureId)
            throws MetaloadException {
        String key = RedisKeysEnum.FT_SENDER.getKey(enclosureId);
        return redisManager.getHgetString(key, SenderKeysEnum.EMAIL.getKey());
    }

    public static boolean isNewSenderEnclosure(RedisManager redisManager, String enclosureId) throws MetaloadException {
        String oldSender = "1";
        String key = RedisKeysEnum.FT_SENDER.getKey(enclosureId);
        String senioritySender = redisManager.getHgetString(key, SenderKeysEnum.IS_NEW.getKey());
        return senioritySender != null && oldSender.equals(senioritySender) ? false : true;
    }

    public static Map<String, String> getRecipientsEnclosure(RedisManager redisManager, String enclosureId) {
        Map<String, String> recipientMap = redisManager.hmgetAllString(RedisKeysEnum.FT_RECIPIENTS.getKey(enclosureId));
        Map<String, String> treeMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        treeMap.putAll(recipientMap);
        return treeMap;
    }

    public static Map<String, String> getRecipientEnclosure(RedisManager redisManager, String recipientId)
            throws MetaloadException {
        return redisManager.hmgetAllString(RedisKeysEnum.FT_RECIPIENT.getKey(recipientId));
    }

    public static String getEnclosureValue(RedisManager redisManager, String enclosureId, String keyMap)
            throws MetaloadException {
        String key = RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId);
        return redisManager.getHgetString(key, keyMap);
    }

    public static Map<String, String> getEnclosure(RedisManager redisManager, String enclosureId)
            throws MetaloadException {
        return redisManager.hmgetAllString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId));
    }

    public static List<String> getRootFiles(RedisManager redisManager, String enclosureId) throws MetaloadException {
        return redisManager.lrange(RedisKeysEnum.FT_ROOT_FILES.getKey(enclosureId), 0, -1);
    }

    public static Map<String, Long> getRootFilesWithSize(RedisManager redisManager, String enclosureId)
            throws MetaloadException {
        Map<String, Long> result = new HashMap<>();
        List<String> rootFiles = getRootFiles(redisManager, enclosureId);
        for (String rootFile : rootFiles) {
            String size = redisManager.getHgetString(
                    RedisKeysEnum.FT_ROOT_FILE.getKey(RedisUtils.generateHashsha1(enclosureId + ":" + rootFile)),
                    RootFileKeysEnum.SIZE.getKey());
            if (size != null && !("".equals(size))) {
                result.put(rootFile, Long.parseLong(size));
            }
        }
        return result;
    }

    public static List<String> getSentPli(RedisManager redisManager, String mail) throws MetaloadException {
        Set<String> pliSet = redisManager.smembersString(RedisKeysEnum.FT_SEND.getKey(mail));
        if (!CollectionUtils.isEmpty(pliSet)) {
            return new ArrayList<String>(pliSet);
        }
        return null;
    }

    public static List<String> getReceivedPli(RedisManager redisManager, String mail) throws MetaloadException {
        Set<String> pliSet = redisManager.smembersString(RedisKeysEnum.FT_RECEIVE.getKey(mail));
        if (!CollectionUtils.isEmpty(pliSet)) {
            return new ArrayList<String>(pliSet);
        }
        return null;
    }

    public static void updateListOfPliSent(RedisManager redisManager, String senderEmail, String enclosureId)
            throws MetaloadException {
        String keyPli = RedisKeysEnum.FT_SEND.getKey(senderEmail);
        redisManager.saddString(keyPli, enclosureId);
    }

    public static void updateListOfPliReceived(RedisManager redisManager, List<String> senderEmail, String enclosureId)
            throws MetaloadException {
        if (!CollectionUtils.isEmpty(senderEmail)) {
            senderEmail.stream().forEach(x -> {
                String keyPli = RedisKeysEnum.FT_RECEIVE.getKey(x);
                redisManager.saddString(keyPli, enclosureId);
            });
        }
    }

    public static List<String> getRootDirs(RedisManager redisManager, String enclosureId) throws MetaloadException {
        return redisManager.lrange(RedisKeysEnum.FT_ROOT_DIRS.getKey(enclosureId), 0, -1);
    }

    public static Map<String, Long> getRootDirsWithSize(RedisManager redisManager, String enclosureId)
            throws MetaloadException {
        Map<String, Long> result = new HashMap<>();
        List<String> rootDirs = getRootDirs(redisManager, enclosureId);
        for (String rootDir : rootDirs) {
            String size = redisManager.getHgetString(
                    RedisKeysEnum.FT_ROOT_DIR.getKey(RedisUtils.generateHashsha1(enclosureId + ":" + rootDir)),
                    RootDirKeysEnum.TOTAL_SIZE.getKey());
            result.put(rootDir, Long.parseLong(size));
        }
        return result;
    }

    public static double getTotalSizeEnclosure(RedisManager redisManager, String enclosureId) throws MetaloadException {
        double totalSize = 0;
        List<String> rootFiles = getRootFiles(redisManager, enclosureId);
        for (String rootFile : rootFiles) {
            String key = RedisKeysEnum.FT_ROOT_FILE.getKey(RedisUtils.generateHashsha1(enclosureId + ":" + rootFile));
            String size = redisManager.getHgetString(key, RootFileKeysEnum.SIZE.getKey());
            if (size != null && !("".equals(size))) {
                totalSize = totalSize + Double.valueOf(size);
            }
        }
        List<String> rootDirs = getRootDirs(redisManager, enclosureId);
        for (String rootDir : rootDirs) {
            String key = RedisKeysEnum.FT_ROOT_DIR.getKey(RedisUtils.generateHashsha1(enclosureId + ":" + rootDir));
            String totalSizeDir = redisManager.getHgetString(key, RootDirKeysEnum.TOTAL_SIZE.getKey());
            if (totalSizeDir != null && !("".equals(totalSizeDir))) {
                totalSize = totalSize + Double.valueOf(totalSizeDir);
            }
        }
        return totalSize;
    }

    // DOWNLOAD API
    public static Integer getNumberOfDownloadsPerRecipient(RedisManager redisManager, String recipientId)
            throws MetaloadException {
        try {
            return Integer.valueOf(redisManager.getHgetString(RedisKeysEnum.FT_RECIPIENT.getKey(recipientId),
                    RecipientKeysEnum.NB_DL.getKey()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static Integer getPasswordTryCountPerRecipient(RedisManager redisManager, String recipientId)
            throws MetaloadException {
        Integer passTry = 0;
        try {
            if (StringUtils.isNotBlank(recipientId)) {
                passTry = Integer.valueOf(redisManager.getHgetString(RedisKeysEnum.FT_RECIPIENT.getKey(recipientId),
                        RecipientKeysEnum.PASSWORD_TRY_COUNT.getKey()));
//				RESET DATE IF TRY AFTER DATE
                LocalDateTime lastTryDate = LocalDateTime.parse(redisManager.getHgetString(
                        RedisKeysEnum.FT_RECIPIENT.getKey(recipientId), RecipientKeysEnum.LAST_PASSWORD_TRY.getKey()));
                if (lastTryDate != null) {
                    LocalDateTime now = LocalDate.now().atStartOfDay();
                    // TODO ADD DELAY ?
                    // On reset le count de try dans redis
                    if (lastTryDate.toLocalDate().atStartOfDay().isBefore(now)) {
                        RedisUtils.resetPasswordTryCountPerRecipient(redisManager, recipientId);
                        return 0;
                    }
                }
            }
        } catch (DateTimeParseException de) {
            redisManager.hsetString(RedisKeysEnum.FT_RECIPIENT.getKey(recipientId),
                    RecipientKeysEnum.LAST_PASSWORD_TRY.getKey(), LocalDateTime.now().toString(), -1);
            return 0;
        } catch (NullPointerException | NumberFormatException e) {
            // Le nombre de try n'existe pas on return 0
            return passTry;
        }
        return passTry;
    }

    public static Boolean isRecipientDeleted(RedisManager redisManager, String recipientId) throws MetaloadException {
        Integer logicDelete = 0;
        Boolean deleted = false;
        try {
            logicDelete = Integer.valueOf(redisManager.getHgetString(RedisKeysEnum.FT_RECIPIENT.getKey(recipientId),
                    RecipientKeysEnum.LOGIC_DELETE.getKey()));
            if (logicDelete == 1) {
                deleted = true;
            }

        } catch (Exception e) {
            // Le nombre de try n'existe pas on return 0
            return false;
        }
        return deleted;
    }

    public static String getHashFileFromredis(RedisManager redisManager, String enclosureId) throws MetaloadException {
        String hashFile = null;
        try {
            hashFile = redisManager.getHgetString(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
                    EnclosureKeysEnum.HASH_FILE.getKey());
        } catch (Exception e) {
            LOGGER.error("File doesnt have hash : " + e.getMessage(), e);
        }
        return hashFile;
    }

    /**
     * Incrémente <strong>atomiquement</strong> le nombre de téléchargements
     * recensés pour un destinataire.
     * 
     * @param redisManager Le service implémentant l interface avec le cache de
     *                     métadonnées.
     * @param recipientId  Le GUID du destinataire qui a téléchargé un pli.
     * @throws MetaloadException Si l'opération d'incrémentation
     */
    public static void incrementNumberOfDownloadsForRecipient(RedisManager redisManager, String recipientId)
            throws MetaloadException {

        try {

            redisManager.hincrBy(RedisKeysEnum.FT_RECIPIENT.getKey(recipientId), RecipientKeysEnum.NB_DL.getKey(), 1);
        } catch (Exception e) {

            throw new MetaloadException(MessageFormat.format(
                    "Echec à l incrémentation du nombre de téléchargements pour le destinataire: {0}", recipientId), e);
        }
    }

    public static void incrementNumberOfDownloadPublic(RedisManager redisManager, String enclosureId)
            throws MetaloadException {
        try {
            redisManager.hincrBy(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
                    EnclosureKeysEnum.PUBLIC_DOWNLOAD_COUNT.getKey(), 1);
        } catch (Exception e) {
            throw new MetaloadException(MessageFormat.format(
                    "Echec à l incrémentation du nombre de téléchargements publiques pour l enclosure : {0}",
                    enclosureId), e);
        }
    }

    public static void incrementNumberOfPasswordTry(RedisManager redisManager, String recipientId)
            throws MetaloadException {
        try {
            if (StringUtils.isNotBlank(recipientId)) {
                redisManager.hincrBy(RedisKeysEnum.FT_RECIPIENT.getKey(recipientId),
                        RecipientKeysEnum.PASSWORD_TRY_COUNT.getKey(), 1);
            }
        } catch (Exception e) {
            throw new MetaloadException(MessageFormat.format(
                    "Echec à l incrémentation d'essai de mot de passe pour le recipient' : {0}", recipientId), e);
        }
    }

    public static void incrementNumberOfCodeTry(RedisManager redisManager, String email) throws MetaloadException {
        try {
            redisManager.hincrBy("sender:" + RedisUtils.generateHashsha1(email.toLowerCase()) + ":", "code-sender-try",
                    1);
        } catch (Exception e) {
            throw new MetaloadException(
                    MessageFormat.format("Echec à l incrémentation d'essai de code pour le mail' : {0}", email), e);
        }
    }

    /**
     * Incrémente <strong>atomiquement</strong> le nombre des fichier(s) uploader
     * pour ce pli.
     * 
     * @param redisManager Le service implémentant l interface avec le cache de
     *                     métadonnées.
     * @param enclosureId  Le GUID du pli.
     * @throws MetaloadException Si l'opération d'incrémentation
     */
    public static Long incrementCounterOfUploadFilesEnclosure(RedisManager redisManager, String enclosureId)
            throws MetaloadException {
        try {
            return redisManager.hincrBy(RedisKeysEnum.FT_ENCLOSURE.getKey(enclosureId),
                    EnclosureKeysEnum.UPLOAD_NB_FILES_DONE.getKey(), 1);
        } catch (Exception e) {
            throw new MetaloadException(MessageFormat.format(
                    "Echec à l incrémentation du nombre des fichier(s) uploader pour ce pli: {0}", enclosureId), e);
        }
    }

    /**
     * Incrémente <strong>atomiquement</strong> le nombre des chunk(s) uploader pour
     * ce fichier.
     * 
     * @param redisManager Le service implémentant l interface avec le cache de
     *                     métadonnées.
     * @param fileId       l identifiant du fichier
     * @throws MetaloadException Si l'opération d'incrémentation
     */
    public static Long incrementCounterOfUploadChunksPerFile(RedisManager redisManager, String fileId)
            throws MetaloadException {
        try {
            return redisManager.hincrBy(RedisKeysEnum.FT_FILE.getKey(fileId), FileKeysEnum.MUL_NB_CHUNKS_DONE.getKey(),
                    1);
        } catch (Exception e) {
            throw new MetaloadException(
                    MessageFormat.format(
                            "Echec à l incrémentation du nombre des chunk(s) uploader pour ce fichier: {0}", fileId),
                    e);
        }
    }

    public static Long getCounterOfChunkIteration(RedisManager redisManager, String fileId) throws MetaloadException {
        try {
            String nbChunk = redisManager.getHgetString(RedisKeysEnum.FT_FILE.getKey(fileId),
                    FileKeysEnum.MUL_CHUNKS_ITERATION.getKey());
            if (StringUtils.isBlank(nbChunk)) {
                return Long.valueOf("0");
            } else {
                return Long.valueOf(nbChunk);
            }
        } catch (Exception e) {
            throw new MetaloadException(
                    MessageFormat.format(
                            "Echec à l incrémentation du compteur des chunk(s) uploader pour ce fichier: {0}", fileId),
                    e);
        }
    }

    public static Long incrementCounterOfChunkIteration(RedisManager redisManager, String fileId)
            throws MetaloadException {
        try {
            return redisManager.hincrBy(RedisKeysEnum.FT_FILE.getKey(fileId),
                    FileKeysEnum.MUL_CHUNKS_ITERATION.getKey(), 1);
        } catch (Exception e) {
            throw new MetaloadException(
                    MessageFormat.format(
                            "Echec à l incrémentation du compteur des chunk(s) uploader pour ce fichier: {0}", fileId),
                    e);
        }
    }

    public static String getRecipientId(RedisManager redisManager, String enclosureId, String mailRecipient)
            throws MetaloadException {
        Map<String, String> recMap = RedisUtils.getRecipientsEnclosure(redisManager, enclosureId);
        return recMap.get(mailRecipient);
    }

    public static boolean checkRecipientMailInEnclosure(RedisManager redisManager, String enclosureId,
            String mailRecipient) throws MetaloadException {
        Map<String, String> recipientMap = RedisUtils.getRecipientsEnclosure(redisManager, enclosureId);
        return recipientMap.containsKey(mailRecipient);
    }

    public static boolean checkRecipientIdInEnclosure(RedisManager redisManager, String enclosureId, String recipientId)
            throws MetaloadException {
        Map<String, String> recipientMap = RedisUtils.getRecipientsEnclosure(redisManager, enclosureId);
        return recipientMap.containsValue(recipientId);
    }

    // =============================================================================================================================================================================

    public static void updateListOfDatesEnclosure(RedisManager redisManager, String enclosureId)
            throws MetaloadException {
        // ================ set dates info in redis ================
        String keyEnclosureDates = RedisKeysEnum.FT_ENCLOSURE_DATES.getKey("");
        String localDate = LocalDate.now().toString();
        redisManager.saddString(keyEnclosureDates, localDate);
        // ================ set dates info in redis ================
        String keyDateEnclosures = RedisKeysEnum.FT_ENCLOSURE_DATE.getKey(localDate);
        redisManager.saddString(keyDateEnclosures, enclosureId);
    }

    public static void resetPasswordTryCountPerRecipient(RedisManager redisManager, String recipientId)
            throws MetaloadException {
        try {
            redisManager.hsetString(RedisKeysEnum.FT_RECIPIENT.getKey(recipientId),
                    RecipientKeysEnum.PASSWORD_TRY_COUNT.getKey(), "0", -1);
        } catch (NumberFormatException e) {
            throw new MetaloadException("value does not exist");
        }
    }

    public static void createListPartEtags(RedisManager redisManager, String shaFid) throws MetaloadException {
        redisManager.insertList(RedisKeysEnum.FT_PART_ETAGS.getKey(shaFid), new ArrayList<>());
    }

    public static void createListIdContainer(RedisManager redisManager, String shaFid) throws MetaloadException {
        redisManager.insertList(RedisKeysEnum.FT_ID_CONTAINER.getKey(shaFid), new ArrayList<>());
    }

    public static List<Integer> getNumberOfPartEtags(RedisManager redisManager, String hashFid)
            throws MetaloadException {
        Pattern pattern = Pattern.compile(":");
        List<Integer> partNumbers = new ArrayList<>();
        getPartEtagsString(redisManager, hashFid).stream().forEach(k -> {
            String[] items = pattern.split(k, 2);
            if (2 == items.length) {
                partNumbers.add(Integer.parseInt(items[0]));
            } else {
                throw new RedisTryAgainException("");
            }

        });
        return partNumbers;
    }

    public static List<String> getPartEtagsString(RedisManager redisManager, String hashFid) throws MetaloadException {
        String key = RedisKeysEnum.FT_PART_ETAGS.getKey(hashFid);
        return redisManager.lrange(key, 0, -1);
    }

    public static List<String> getFilesIds(RedisManager redisManager, String enclosureId) throws MetaloadException {
        String key = RedisKeysEnum.FT_FILES_IDS.getKey(enclosureId);
        return redisManager.lrange(key, 0, -1);
    }

    public static Map<String, String> getFileInfo(RedisManager redisManager, String hashFid) throws MetaloadException {
        return redisManager.hmgetAllString(RedisKeysEnum.FT_FILE.getKey(hashFid));
    }

    public static void addPliToDay(RedisManager redisManager, String senderId, String enclosureId) {
        String senderKey = RedisKeysEnum.FT_SENDER_PLIS.getKey(senderId);
        redisManager.saddString(senderKey, enclosureId);
        int seconds = Math
                .toIntExact(Duration.between(LocalDateTime.now(), LocalDate.now().atTime(LocalTime.MAX)).getSeconds());
        redisManager.expire(senderKey, seconds);
    }

}
