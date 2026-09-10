import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]


class RecoveryTrainingScriptTest(unittest.TestCase):
    def command(self, **overrides):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            capture = root / 'arguments.json'
            python = root / 'capture-python'
            python.write_text(
                f'#!{sys.executable}\n'
                'import json, os, sys\n'
                'from pathlib import Path\n'
                'Path(os.environ["RECOVERY_TEST_CAPTURE"]).write_text(json.dumps(sys.argv[1:]))\n'
            )
            python.chmod(0o755)
            output = root / 'policy.json'
            output.write_text('{}')
            jar = root / 'runtime.jar'
            jar.touch()
            env = {key: value for key, value in os.environ.items() if not key.startswith('RL_')}
            env.update(RL_BUILD_BEFORE_TRAINING='0', RL_PYTHON=str(python), RL_JAR=str(jar),
                       RL_RECOVERY_CHECKPOINT_DIR=str(root / 'checkpoints'),
                       RL_RECOVERY_OUTPUT=str(output), RL_RECOVERY_INIT_POLICY=str(output),
                       RECOVERY_TEST_CAPTURE=str(capture))
            env.update(overrides)
            subprocess.run(['bash', str(ROOT / 'tools/rl/train_recovery.sh')],
                           env=env, check=True, capture_output=True, text=True)
            return json.loads(capture.read_text())

    def test_default_uses_scaled_separate_critic_and_installed_actor(self):
        args = self.command()
        self.assertIn('--separate-value-network', args)
        self.assertEqual('0.001', args[args.index('--learner-reward-scale') + 1])
        self.assertEqual('1.0', args[args.index('--vf-loss-coeff') + 1])
        self.assertIn('--init-policy', args)
        self.assertNotIn('--resume', args)

    def test_environment_can_override_learner_settings(self):
        args = self.command(RL_RECOVERY_SEPARATE_VALUE_NETWORK='0',
                            RL_RECOVERY_LEARNER_REWARD_SCALE='0.002')
        self.assertNotIn('--separate-value-network', args)
        self.assertEqual('0.002', args[args.index('--learner-reward-scale') + 1])


if __name__ == '__main__':
    unittest.main()
