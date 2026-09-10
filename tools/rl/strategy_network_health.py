"""Bounded, read-only diagnostics for card-strategy networks."""

import torch
from torch import nn


def network_health(network, observations, sample_limit=2048):
    values = observations.detach().reshape(-1, observations.shape[-1])
    stride = max(1, (len(values) + sample_limit - 1) // sample_limit)
    values = values[::stride][:sample_limit]
    result = {"samples": len(values), "layers": []}
    if not len(values):
        return result
    with torch.no_grad():
        for layer in network.network:
            values = layer(values)
            if isinstance(layer, nn.Tanh):
                saturated = values.abs() >= 0.99
                result["layers"].append({
                    "saturated_fraction": saturated.float().mean().item(),
                    "persistently_saturated_neurons": (
                        saturated.float().mean(dim=0) >= 0.95
                    ).sum().item(),
                    "neurons": values.shape[-1],
                    "mean_tanh_derivative": (1 - values.square()).mean().item(),
                    "mean_input_variation": values.std(dim=0, unbiased=False).mean().item(),
                    "finite": bool(torch.isfinite(values).all()),
                })
    result["gradient_norms"] = [
        {"parameter": name, "norm": parameter.grad.norm().item(),
         "finite": bool(torch.isfinite(parameter.grad).all())}
        for name, parameter in network.named_parameters()
        if parameter.grad is not None
    ]
    return result
