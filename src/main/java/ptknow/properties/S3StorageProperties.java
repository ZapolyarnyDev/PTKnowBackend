package ptknow.properties;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class S3StorageProperties {

    String endpoint;

    String region = "us-east-1";

    String bucket;

    String accessKey;

    String secretKey;

    boolean pathStyleAccessEnabled = true;

    String keyPrefix;
}
