package agence;

import agence.cli.AgencyCli;
import agence.client.AgencyConfig;
import agence.service.AgencyService;

public class AgencyApp {

    public static void main(String[] args) {
        AgencyConfig cfg = new AgencyConfig();
        AgencyService svc = new AgencyService(cfg.getAgencyId(), cfg.getAgencySecret(), cfg.getHotelUrls());

        AgencyCli cli = new AgencyCli(svc);
        cli.run();
    }
}
