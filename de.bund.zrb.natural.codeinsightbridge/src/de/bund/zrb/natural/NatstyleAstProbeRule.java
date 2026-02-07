package de.bund.zrb.natural;

import com.softwareag.naturalone.natural.natstyle.NATstyleCheckerParserImpl;

import de.bund.zrb.natural.infrastructure.logging.PluginLog;

@SuppressWarnings("restriction")
public class NatstyleAstProbeRule extends NATstyleCheckerParserImpl {

    @Override
    public void runCheck() {
        PluginLog.info("NatstyleAstProbeRule started.");

        NaturalAstRoot astRoot = resolveAstRoot();
        if (astRoot == null) {
            PluginLog.error("AST root is null. Parser access failed.", null);
            return;
        }

        logBasicAstInfo(astRoot);
    }

    private NaturalAstRoot resolveAstRoot() {
        try {
            // TODO: Use real NATstyle API – typische Idee:
            // return getNaturalParser().getNaturalASTRoot();
            return getNaturalParser().getNaturalASTRoot();
        } catch (Exception e) {
            PluginLog.error("Failed to obtain AST root from NATstyle parser.", e);
            return null;
        }
    }

    private void logBasicAstInfo(NaturalAstRoot astRoot) {
        StringBuilder builder = new StringBuilder();
        builder.append("AST root obtained: ");
        builder.append(astRoot.getClass().getName());

        // TODO: Add more info if API allows it
        // e.g. number of children, source range, etc.

        PluginLog.info(builder.toString());
    }

	@Override
	public void initParameterList() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String run() {
		// TODO Auto-generated method stub
		return null;
	}
}
