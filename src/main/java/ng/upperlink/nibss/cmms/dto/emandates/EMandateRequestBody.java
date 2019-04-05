package ng.upperlink.nibss.cmms.dto.emandates;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EMandateRequestBody {
   private AuthParam auth;
   private EmandateRequest emandateRequest;
}

